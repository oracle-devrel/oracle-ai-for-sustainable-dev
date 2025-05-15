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
 
package com.example.hotelsb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.hotelsb.model.Booking;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HotelService {
    private Map<String, Booking> bookings = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(HotelResource.class);

    public Booking book(String bookingId, String hotel, Connection connection) {
        Booking booking = new Booking(bookingId, hotel, "Hotel", null);

        try (Statement s = connection.createStatement()) {
            s.executeUpdate("UPDATE hotels SET seats = seats - 1 WHERE id = 1");
            // Local commit for the decrement
            connection.commit();
        } catch (SQLException ex) {
            booking.setStatus(Booking.BookingStatus.FAILED);
            LOG.error("Error while reserving hotel seat errCode", ex);
        }
        Booking earlierBooking = bookings.putIfAbsent(booking.getId(), booking);
        return earlierBooking == null ? booking : earlierBooking;
    }

    public Booking get(String bookingId) throws ResponseStatusException {
        if (!bookings.containsKey(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Booking id: " + bookingId);
        }
        return bookings.get(bookingId);
    }

    public Collection<Booking> getAll() {
        return bookings.values();
    }

    public void setMaxBooking(int count, Connection connection) {
        int exisCnt = -1;
        String query = "SELECT * FROM hotels where id=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, 1);
            ResultSet dataSet = statement.executeQuery();
            if (dataSet.next()) {
                exisCnt = dataSet.getInt("seats");
            }
        } catch (SQLException ex) {
            LOG.error("Error while setting max booking value", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

        try (Statement s = connection.createStatement()) {
            if (exisCnt  == -1) {
                query = "insert into hotels (id, seats) values (1, " + Integer.valueOf(count) + " )";
                s.execute(query);
            } else if (exisCnt > count) {
                query = "UPDATE hotels SET seats = seats  - " + Integer.valueOf(exisCnt - count) + " WHERE id = 1";
                s.execute(query);
            } else if (exisCnt < count) {
                query = "UPDATE hotels SET seats = seats  + " + Integer.valueOf(count - exisCnt ) + " WHERE id = 1";
                s.execute(query);
            }
        } catch (SQLException ex) {
            LOG.error("Error while setting max booking value", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
}
