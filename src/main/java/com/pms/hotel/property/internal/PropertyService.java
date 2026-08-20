package com.pms.hotel.property.internal;

import com.pms.hotel.property.PropertyApi;
import com.pms.hotel.property.PropertySummary;
import com.pms.hotel.property.internal.web.PropertyRequests.CreatePropertyRequest;
import com.pms.hotel.property.internal.web.PropertyRequests.UpdatePropertyRequest;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService implements PropertyApi {

    private final PropertyRepository propertyRepository;
    private final UserPropertyAccessRepository userPropertyAccessRepository;

    @Override
    @Transactional(readOnly = true)
    public PropertySummary getById(Long propertyId) {
        return findEntity(propertyId).toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertySummary> findAllActive() {
        return propertyRepository.findByActiveTrueOrderByName().stream().map(Property::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAllActivePropertyIds() {
        return propertyRepository.findByActiveTrueOrderByName().stream().map(Property::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findGrantedPropertyIds(Long userId) {
        return userPropertyAccessRepository.findByUserId(userId).map(UserPropertyAccess::getPropertyId).map(List::of).orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<Property> findAll() {
        return propertyRepository.findAll();
    }

    public Property create(CreatePropertyRequest request) {
        if (propertyRepository.existsByCode(request.code())) {
            throw new BusinessRuleException("Le code \"" + request.code() + "\" est déjà utilisé par un autre établissement.");
        }
        Property property = new Property();
        property.setName(request.name());
        property.setCode(request.code());
        property.setAddress(request.address());
        return propertyRepository.save(property);
    }

    public Property update(Long id, UpdatePropertyRequest request) {
        Property property = findEntity(id);
        if (request.name() != null) property.setName(request.name());
        if (request.address() != null) property.setAddress(request.address());
        if (request.active() != null) property.setActive(request.active());
        return propertyRepository.save(property);
    }

    /** Ids des utilisateurs ayant un accès explicite à cet établissement (voir UserPropertyAccess). */
    @Transactional(readOnly = true)
    public List<Long> listGrantedUserIds(Long propertyId) {
        findEntity(propertyId); // 404 si l'établissement n'existe pas
        return userPropertyAccessRepository.findByPropertyId(propertyId).stream().map(UserPropertyAccess::getUserId).toList();
    }

    /**
     * Affecte cet utilisateur à cet établissement — remplace toute affectation
     * précédente : un membre du personnel n'est jamais rattaché qu'à un seul
     * établissement à la fois (voir la contrainte unique(user_id)), justement
     * pour qu'il n'ait jamais à en choisir un lors d'une réservation.
     */
    public void grantAccess(Long propertyId, Long userId) {
        findEntity(propertyId); // 404 si l'établissement n'existe pas
        UserPropertyAccess access = userPropertyAccessRepository.findByUserId(userId).orElseGet(UserPropertyAccess::new);
        access.setUserId(userId);
        access.setPropertyId(propertyId);
        userPropertyAccessRepository.save(access);
    }

    /** Sans effet si l'utilisateur est actuellement affecté à un AUTRE établissement que celui-ci. */
    public void revokeAccess(Long propertyId, Long userId) {
        userPropertyAccessRepository.findByUserId(userId)
                .filter(access -> access.getPropertyId().equals(propertyId))
                .ifPresent(userPropertyAccessRepository::delete);
    }

    Property findEntity(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Établissement", id));
    }
}
