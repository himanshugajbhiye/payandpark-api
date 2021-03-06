package com.payandpark.payandpark.booking.service;

import com.payandpark.payandpark.booking.model.Booking;
import com.payandpark.payandpark.booking.model.BookingStatus;
import com.payandpark.payandpark.booking.model.CreateBookingRequest;
import com.payandpark.payandpark.booking.model.FetchBookingsRequest;
import com.payandpark.payandpark.booking.repository.BookingRepository;
import com.payandpark.payandpark.charge.model.Charge;
import com.payandpark.payandpark.charge.model.Constants;
import com.payandpark.payandpark.charge.service.ChargingService;
import com.payandpark.payandpark.exception.BadRequestException;
import com.payandpark.payandpark.parkingslot.model.ParkingSlot;

import static com.payandpark.payandpark.parkingslot.model.ParkingSlotStatus.*;

import com.payandpark.payandpark.parkingslot.service.ParkingSlotService;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Log4j2
@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    BookingRepository repository;

    @Autowired
    ParkingSlotService parkingSlotService;

    @Autowired
    ChargingService chargingService;

    @Override
    public Booking createBooking(CreateBookingRequest request) {
        log.info("Creating booking for request :: {}", request.toString());
        ParkingSlot parkingSlot = parkingSlotService.fetchParkingSlotById(request.getParkingSlotId());

        if (UNAVAILABLE.name().equalsIgnoreCase(parkingSlot.getStatus())
                || BOOKED.name().equalsIgnoreCase(parkingSlot.getStatus())) {
            log.info("Parking slot :: {} is :: {}", request.getParkingSlotId(), parkingSlot.getStatus());
            throw new BadRequestException("Parking slot :: " + request.getParkingSlotId() + " is :: " + parkingSlot.getStatus());
        }

        Booking booking = repository.createBooking(request);
        parkingSlotService.updateParkingSlotStatus(request.getParkingSlotId(), BOOKED.name());
        return booking;
    }

    @Override
    public Booking endBooking(int bookingId) {
        log.info("Ending booking :: {}", bookingId);
        Booking booking = repository.endBooking(bookingId);
        ParkingSlot parkingSlot = parkingSlotService.fetchParkingSlotById(booking.getParkingSlotId());
        Charge charge = chargingService.fetchChargeByVehicleTypeId(parkingSlot.getVehicleTypeId());

        Timestamp startTime = Timestamp.valueOf(booking.getStartTime());
        Timestamp endTime = Timestamp.valueOf(booking.getEndTime());
        long bookingMinutes = (endTime.getTime() - startTime.getTime())/60000;
        log.info("Booking :: {}, minutes :: {}, price per minute :: {}", bookingId, bookingMinutes, charge.getPricePerMinute());

        int bookingPrice = (int)Math.floor(charge.getPricePerMinute() * bookingMinutes);
        bookingPrice = Math.round(Math.max(Constants.MIN_BOOKING_MINUTES * charge.getPricePerMinute(), bookingPrice));
        log.info("Booking price :: {}", bookingPrice);

        booking = repository.updateBookingPrice(bookingId, bookingPrice);
        parkingSlotService.updateParkingSlotStatus(booking.getParkingSlotId(), AVAILABLE.name());

        return booking;
    }

    @Override
    public Booking fetchBookingDetailsById(int bookingId) {
        log.info("Fetching booking details for id :: {}", bookingId);
        return repository.fetchBookingDetailsById(bookingId);
    }

    @Override
    public List<Booking> fetchAllBookings(FetchBookingsRequest request) {

        if (request.getUserId() == null && !StringUtils.hasText(request.getStatus())) {
            log.error("All parameters are null or empty for request :: {}", request.toString());
            throw new BadRequestException("All parameters are null or empty for request :: " + request.toString());
        }

        if (StringUtils.hasText(request.getStatus())) {
            String status = request.getStatus().toUpperCase();
            return fetchAllBookingsByStatus(status);
        } else if (request.getUserId() != null) {
            return fetchAllBookingsByUserId(request.getUserId());
        }

        log.error("Invalid fetch bookings request :: {}", request.toString());
        throw new BadRequestException("Invalid fetch bookings request :: " + request.toString());
    }

    private List<Booking> fetchAllBookingsByStatus(String status) {
        log.info("Fetching booking details by status :: {}", status);
        switch (BookingStatus.valueOf(status)) {
            case ALL:
                return repository.fetchAllBookings();

            case ACTIVE:
            case ENDED:
                log.info("Fetching {} bookings", status);
                return repository.fetchAllBookingsByStatus(status);

            default:
                log.info("Undefined booking status :: {}", status);
                throw new BadRequestException("Undefined booking status :: " + status);
        }
    }

    private List<Booking> fetchAllBookingsByUserId(int userId) {
        log.info("Fetching booking details for userId :: {}", userId);
        return repository.fetchAllBookingsByUserId(userId);
    }
}
