package com.example.restexample.events;

import com.example.restexample.accounts.Account;
import com.example.restexample.accounts.AccountAdapter;
import com.example.restexample.accounts.CurrentUser;
import com.example.restexample.index.IndexController;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Controller
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_JSON_VALUE)
public class EventController {
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final EventValidator eventValidator;

    public EventController(EventRepository eventRepository, ModelMapper modelMapper, EventValidator eventValidator){
        this.eventRepository = eventRepository;
        this.modelMapper = modelMapper;
        this.eventValidator = eventValidator;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody @Valid EventDto eventDto,
                                         Errors errors,
                                         @CurrentUser Account currentUser){
        if(errors.hasErrors()){
            return this.badRequest(errors);
        }

        eventValidator.validate(eventDto, errors);
        if(errors.hasErrors()){
            return this.badRequest(errors);
        }

        Event event = modelMapper.map(eventDto, Event.class);
        event.update();
        event.setManager(currentUser);

        Event newEvent = this.eventRepository.save(event);
        WebMvcLinkBuilder webMvcLinkBuilder = linkTo(EventController.class).slash(newEvent.getId());
        URI createdUri = webMvcLinkBuilder.toUri();

        EventEntityModel eventRepresentationModel = new EventEntityModel(event);
        eventRepresentationModel.add(linkTo(EventController.class).withRel("query-events"));
        eventRepresentationModel.add(webMvcLinkBuilder.withRel("update-event"));
        eventRepresentationModel.add(Link.of("/docs/index.html#resources-events-create").withRel("profile"));

        return ResponseEntity.created(createdUri).body(eventRepresentationModel);
    }

    @GetMapping
    public ResponseEntity<?> queryEvents(Pageable pageable,
                                         PagedResourcesAssembler<Event> assembler,
                                         @CurrentUser Account account) {
        Page<Event> page = this.eventRepository.findAll(pageable);
        var pagedResources = assembler.toModel(page, e-> new EventEntityModel(e));
        pagedResources.add(Link.of("/docs/index.html#resources-events-list").withRel("profile"));

        if (account != null) {
            pagedResources.add(linkTo(EventController.class).withRel("create-event"));
        }

        return ResponseEntity.ok(pagedResources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable Integer id,
                                      @CurrentUser Account currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);
        if(optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Event event = optionalEvent.get();
        EventEntityModel eventEntityModel = new EventEntityModel(event);
        eventEntityModel.add(Link.of("/resources-events-get").withRel("profile"));

        if (event.getManager().equals(currentUser)) {
            eventEntityModel.add(linkTo(EventController.class).slash(event.getId()).withRel("update-event"));
        }

        return ResponseEntity.ok(eventEntityModel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Integer id,
                                         @RequestBody @Valid EventDto eventDto,
                                         Errors errors,
                                         @CurrentUser Account currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);
        if (optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (errors.hasErrors()) {
            return badRequest(errors);
        }

        this.eventValidator.validate(eventDto, errors);
        if(errors.hasErrors()) {
            return badRequest(errors);
        }

        Event existingEvent = optionalEvent.get();
        if (existingEvent.getManager().equals(currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        this.modelMapper.map(eventDto, existingEvent);
        Event savedEvent = this.eventRepository.save(existingEvent);

        EventEntityModel eventEntityModel = new EventEntityModel(savedEvent);
        eventEntityModel.add(Link.of("/docs/index.html#resources-events-list").withRel("profile"));

        return ResponseEntity.ok(eventEntityModel);
    }

    private ResponseEntity<EntityModel<Errors>> badRequest(Errors errors) {
        EntityModel<Errors> entityModel = EntityModel.of(errors);
        entityModel.add(linkTo(methodOn(IndexController.class).index()).withRel("index"));

        return ResponseEntity.badRequest().body(entityModel);
    }
}
