# Design decisions

Spring WebFlux - provides scalability and can handle high loads

[//]: # (TODO review)
Tests enforce 1.5 seconds SLA per batch of requests sent to the APIs (pricing, tracking, shipment) - 3 x 1.5 = 4.5 seconds => lower than the required SLA of 5s

