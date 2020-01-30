package ua.org.java.dynamoit.tables;

import org.junit.jupiter.api.Test;

public class DynamoTest {

    @Test
    public void testTables() {
//        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
//        ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
//
//        System.out.println(listTablesResponse.tableNames());
    }

    @Test
    public void testCopyVenue() {
//        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
//        Set<String> venueIds = dynamoDbClient.scan(ScanRequest.builder().tableName("oms.prod.venues").build())
//                .items()
//                .stream()
//                .map(item -> item.get("id"))
//                .filter(Objects::nonNull)
//                .map(AttributeValue::s)
//                .collect(Collectors.toSet());
//        ScanResponse excursions = dynamoDbClient.scan(ScanRequest.builder().tableName("excursions").build());
//
//        Set<Map<String, AttributeValue>> broken = excursions.items().stream()
//                .filter(item -> Optional.ofNullable(item.get("venueId"))
//                        .map(AttributeValue::s)
//                        .map(id -> !venueIds.contains(id))
//                        .orElse(true))
//                .collect(Collectors.toSet());
//
//        broken.forEach(item->{
//            System.out.println(item.get("id"));
//        });
    }

}
