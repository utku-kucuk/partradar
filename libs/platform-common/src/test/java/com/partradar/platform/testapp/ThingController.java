package com.partradar.platform.testapp;

import com.partradar.platform.web.BusinessRuleException;
import com.partradar.platform.web.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ThingController {

  record NewThing(@NotBlank String name) {}

  @GetMapping("/things/{id}")
  String find(@PathVariable long id) {
    if (id == 0) {
      throw new BusinessRuleException("thing-zero", "Thing 0 is reserved");
    }
    throw new NotFoundException("Thing", id);
  }

  @PostMapping("/things")
  String create(@Valid @RequestBody NewThing thing) {
    return thing.name();
  }
}
