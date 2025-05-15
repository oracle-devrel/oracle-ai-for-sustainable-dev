/*
Copyright (c) 2023, Oracle and/or its affiliates. **

The Universal Permissive License (UPL), Version 1.0 **

Subject to the condition set forth below, permission is hereby granted to any person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright rights in the Software, and any and all patent rights owned or freely licensable by each
licensor hereunder covering either the unmodified Software as contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both **
(a) the Software, and (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the Software (each a "Larger Work" to which the
Software is contributed by such licensors), **
without restriction, including without limitation the rights to copy, create derivative works of, display, perform, and distribute the Software and make, use, sell,
offer for sale, import, export, have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights on either these or other terms. **

This license is subject to the following condition: The above copyright notice and either this complete permission notice or at a minimum a reference to the UPL must be
included in all copies or substantial portions of the Software. **

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.example.tripmanagersb.service;

import com.example.tripmanagersb.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Component
public class BookingServiceImpl implements BookingService {

    /**
     * RestTemplate must be autowired because then only the client Interceptor will be able to intercept
     * Interceptors are specific to the restTemplate
     */
    @Autowired
    @Qualifier("MicroTxLRA")
    private RestTemplate restTemplate;

    @Value("${hotel.service.url}")
    private String hotelServiceUri;

    @Value("${flight.service.url}")
    private String flightServiceUri;

    private static final Logger LOG = LoggerFactory.getLogger(BookingServiceImpl.class);

    @Async(value = "taskExecutorForTripBooking")
    @Override
    public CompletableFuture bookHotel(String name, String id) {
        Booking hotelBooking = null;
        LOG.debug("Calling Hotel Service to book hotel with booking Id : " + id);
        URI hotelUri = getHotelTarget()
                .queryParam("hotelName", name)
                .build()
                .toUri();

        hotelBooking = restTemplate.postForEntity(hotelUri, null, Booking.class).getBody();

        assert hotelBooking != null;
        LOG.info(String.format("Hotel booking %s with booking Id : %s", (hotelBooking.getStatus() == Booking.BookingStatus.FAILED ? "FAILED" : "SUCCESSFUL"), hotelBooking.getId()));
        return CompletableFuture.completedFuture(hotelBooking);
    }

    @Async(value = "taskExecutorForTripBooking")
    @Override
    public CompletableFuture bookFlight(String flightNumber, String id) {
        Booking flightBooking = null;
        LOG.debug("Calling Flight Service to book flight with booking Id : " + id);

        URI flightUri = getFlightTarget()
                .queryParam("flightNumber", flightNumber)
                .build()
                .toUri();

        flightBooking = restTemplate.postForEntity(flightUri, null, Booking.class).getBody();

        assert flightBooking != null;
        LOG.info(String.format("Flight booking %s with booking Id : %s", (flightBooking.getStatus() == Booking.BookingStatus.FAILED ? "FAILED" : "SUCCESSFUL"), flightBooking.getId()));
        return CompletableFuture.completedFuture(flightBooking);
    }

    private UriComponentsBuilder getHotelTarget() {
        return UriComponentsBuilder.fromUri(URI.create(hotelServiceUri));
    }

    private UriComponentsBuilder getFlightTarget() {
        return UriComponentsBuilder.fromUri(URI.create(flightServiceUri));
    }
}
