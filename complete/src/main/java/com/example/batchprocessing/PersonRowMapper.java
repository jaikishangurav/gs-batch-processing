package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonRowMapper implements RowMapper<Person> {
    public static final String FIRSTNAME_COLUMN = "first_name";
    public static final String LASTNAME_COLUMN = "last_name";
    public static final String CREDIT_COLUMN = "credit";

    private static final Logger log = LoggerFactory.getLogger(PersonRowMapper.class);

    public Person mapRow(ResultSet rs, int rowNum) throws SQLException {
        Person person = new Person();

        person.setFirstName(rs.getString(FIRSTNAME_COLUMN));

        person.setLastName(rs.getString(LASTNAME_COLUMN));
        //person.setCredit(rs.getBigDecimal(CREDIT_COLUMN));
        log.info("Row Mapper");
        log.info(person.toString());

        return person;
    }
}