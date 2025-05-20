CREATE TABLE tripcom_flights_HKG (
    id NUMBER PRIMARY KEY,
    airline VARCHAR2(100),
    flight_time VARCHAR2(50),
    price NUMBER,
    flight_number VARCHAR2(50),
    departure VARCHAR2(10),
    arrival VARCHAR2(10)
);

CREATE SEQUENCE tripcom_seq START WITH 1 INCREMENT BY 1;
