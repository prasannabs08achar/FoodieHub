package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.BatchingConfigDao;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.model.BatchingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BatchRouteValidationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final BatchingConfigDao batchingConfigDao;

    /**
     * Verifies that every pair of customer drop-off locations
     * is within the configured batching radius.
     *
     * @param orders orders being considered for batching
     * @return true when every pair is within the configured radius
     */
    public boolean areOrdersWithinBatchRadius(
            List<DispatchOrderResponse> orders
    ) {

        if (orders == null || orders.size() < 2) {
            return false;
        }

        BatchingConfig config =
                getConfig();

        BigDecimal radiusKm =
                config.getBatchRadiusKm();

        if (radiusKm == null
                || radiusKm.compareTo(BigDecimal.ZERO) <= 0) {

            return false;
        }

        for (int i = 0; i < orders.size(); i++) {

            DispatchOrderResponse first =
                    orders.get(i);

            if (!hasValidCoordinates(first)) {
                return false;
            }

            for (int j = i + 1; j < orders.size(); j++) {

                DispatchOrderResponse second =
                        orders.get(j);

                if (!hasValidCoordinates(second)) {
                    return false;
                }

                BigDecimal distance =
                        calculateDistance(
                                first.deliveryLatitude(),
                                first.deliveryLongitude(),
                                second.deliveryLatitude(),
                                second.deliveryLongitude()
                        );

                if (distance.compareTo(radiusKm) > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates the additional route distance introduced by batching.
     *
     * The earliest order is used as the anchor. The route starts from
     * that customer's drop-off and visits the remaining drop-offs using
     * a nearest-neighbour strategy.
     *
     * The resulting route is compared with the direct distance from
     * the earliest customer's drop-off to the farthest stop.
     *
     * @param orders orders in earliest-first order
     * @return true when the calculated detour is within the configured threshold
     */
    public boolean isDetourAcceptable(
            List<DispatchOrderResponse> orders
    ) {

        if (orders == null || orders.size() < 2) {
            return false;
        }

        if (orders.stream()
                .anyMatch(order -> !hasValidCoordinates(order))) {

            return false;
        }

        BatchingConfig config =
                getConfig();

        BigDecimal maxDetourPercentage =
                config.getMaxDetourPercentage();

        if (maxDetourPercentage == null
                || maxDetourPercentage.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            return false;
        }

        /*
         * With exactly two orders, the batch route between the
         * two customer locations is the same as the direct route.
         * Therefore there is no additional routing detour.
         */
        if (orders.size() == 2) {
            return true;
        }

        DispatchOrderResponse earliest =
                orders.get(0);

        BigDecimal directDistance =
                findDirectDistanceToFarthestStop(
                        earliest,
                        orders
                );

        if (directDistance.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        BigDecimal batchRouteDistance =
                calculateNearestNeighbourRouteDistance(
                        orders
                );

        BigDecimal additionalDistance =
                batchRouteDistance
                        .subtract(directDistance)
                        .max(BigDecimal.ZERO);

        BigDecimal detourPercentage =
                additionalDistance
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                directDistance,
                                4,
                                RoundingMode.HALF_UP
                        );

        return detourPercentage.compareTo(
                maxDetourPercentage
        ) <= 0;
    }

    /**
     * Finds the direct distance from the earliest customer's
     * drop-off to the farthest drop-off in the candidate batch.
     */
    private BigDecimal findDirectDistanceToFarthestStop(
            DispatchOrderResponse earliest,
            List<DispatchOrderResponse> orders
    ) {

        BigDecimal farthestDistance =
                BigDecimal.ZERO;

        for (int i = 1; i < orders.size(); i++) {

            DispatchOrderResponse order =
                    orders.get(i);

            BigDecimal distance =
                    calculateDistance(
                            earliest.deliveryLatitude(),
                            earliest.deliveryLongitude(),
                            order.deliveryLatitude(),
                            order.deliveryLongitude()
                    );

            if (distance.compareTo(farthestDistance) > 0) {
                farthestDistance = distance;
            }
        }

        return farthestDistance;
    }

    /**
     * Calculates the route beginning at the earliest customer's
     * drop-off and visiting all remaining stops using a
     * nearest-neighbour strategy.
     */
    private BigDecimal calculateNearestNeighbourRouteDistance(
            List<DispatchOrderResponse> orders
    ) {

        if (orders.size() < 2) {
            return BigDecimal.ZERO;
        }

        DispatchOrderResponse current =
                orders.get(0);

        Set<Integer> visited =
                new HashSet<>();

        visited.add(0);

        BigDecimal totalDistance =
                BigDecimal.ZERO;

        while (visited.size() < orders.size()) {

            int nearestIndex = -1;

            BigDecimal nearestDistance = null;

            for (int i = 0; i < orders.size(); i++) {

                if (visited.contains(i)) {
                    continue;
                }

                DispatchOrderResponse candidate =
                        orders.get(i);

                BigDecimal distance =
                        calculateDistance(
                                current.deliveryLatitude(),
                                current.deliveryLongitude(),
                                candidate.deliveryLatitude(),
                                candidate.deliveryLongitude()
                        );

                if (nearestDistance == null
                        || distance.compareTo(
                        nearestDistance
                ) < 0) {

                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }

            if (nearestIndex == -1
                    || nearestDistance == null) {

                break;
            }

            totalDistance =
                    totalDistance.add(
                            nearestDistance
                    );

            visited.add(nearestIndex);

            current =
                    orders.get(nearestIndex);
        }

        return totalDistance;
    }

    /**
     * Haversine distance between two latitude/longitude points.
     */
    private BigDecimal calculateDistance(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {

        double lat1 =
                Math.toRadians(
                        latitude1.doubleValue()
                );

        double lon1 =
                Math.toRadians(
                        longitude1.doubleValue()
                );

        double lat2 =
                Math.toRadians(
                        latitude2.doubleValue()
                );

        double lon2 =
                Math.toRadians(
                        longitude2.doubleValue()
                );

        double deltaLat =
                lat2 - lat1;

        double deltaLon =
                lon2 - lon1;

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return BigDecimal
                .valueOf(
                        EARTH_RADIUS_KM * c
                )
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private boolean hasValidCoordinates(
            DispatchOrderResponse order
    ) {

        if (order == null) {
            return false;
        }

        if (order.deliveryLatitude() == null
                || order.deliveryLongitude() == null) {

            return false;
        }

        double latitude =
                order.deliveryLatitude()
                        .doubleValue();

        double longitude =
                order.deliveryLongitude()
                        .doubleValue();

        return latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private BatchingConfig getConfig() {

        return batchingConfigDao
                .findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Batching configuration not found"
                        )
                );
    }
}