package com.pms.hotel.property.internal.web;

import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.property.PropertySummary;
import com.pms.hotel.property.internal.Property;
import com.pms.hotel.property.internal.PropertyService;
import com.pms.hotel.property.internal.web.PropertyRequests.CreatePropertyRequest;
import com.pms.hotel.property.internal.web.PropertyRequests.GrantAccessRequest;
import com.pms.hotel.property.internal.web.PropertyRequests.UpdatePropertyRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
class PropertyController {

    private final PropertyService propertyService;
    private final CurrentProperty currentProperty;

    @GetMapping
    public List<Property> index() {
        return propertyService.findAll();
    }

    /** Établissements accessibles à l'appelant courant — ce que le sélecteur d'établissement du frontend doit charger. */
    @GetMapping("/mine")
    public List<PropertySummary> mine() {
        return currentProperty.accessibleProperties();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public Property store(@Valid @RequestBody CreatePropertyRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public Property update(@PathVariable Long id, @Valid @RequestBody UpdatePropertyRequest request) {
        return propertyService.update(id, request);
    }

    @GetMapping("/{id}/access")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public List<Long> listAccess(@PathVariable Long id) {
        return propertyService.listGrantedUserIds(id);
    }

    /** Établissements accordés à un utilisateur donné — pour pré-cocher le formulaire de création/édition du personnel. */
    @GetMapping("/staff/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public List<Long> listAccessForUser(@PathVariable Long userId) {
        return propertyService.findGrantedPropertyIds(userId);
    }

    @PostMapping("/{id}/access")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public void grantAccess(@PathVariable Long id, @Valid @RequestBody GrantAccessRequest request) {
        propertyService.grantAccess(id, request.userId());
    }

    @DeleteMapping("/{id}/access/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER-ADMIN')")
    public void revokeAccess(@PathVariable Long id, @PathVariable Long userId) {
        propertyService.revokeAccess(id, userId);
    }
}
