package com.example.restexample.events;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;

@Component
public class EventValidator {

    public void validate(EventDto eventDto, Errors errors){
        if(eventDto.getBasePrice() > eventDto.getMaxPrice() && eventDto.getMaxPrice() != 0){
            errors.rejectValue("basePrice", "wrongValue", "Base price is wrong");
            errors.rejectValue("maxPrice", "wrongValue", "Max price is wrong");

            LocalDateTime endEventDateTIme = eventDto.getEndEventDateTime();
            if(endEventDateTIme.isBefore(eventDto.getBeginEventDateTime()) ||
            endEventDateTIme.isBefore(eventDto.getCloseEnrollmentDateTime()) ||
            endEventDateTIme.isBefore(eventDto.getBeginEnrollmentDateTime())) {
                errors.rejectValue("endEventDateTime", "wrongValue", "End event date time is wrong");
            }

            // ToDO BeginEventDateTime
            // ToDo CloseEnrollmentDateTime
        }
    }
}
