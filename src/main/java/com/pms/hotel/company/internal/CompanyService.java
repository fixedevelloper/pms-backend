package com.pms.hotel.company.internal;

import com.pms.hotel.company.CompanyApi;
import com.pms.hotel.company.CompanySummary;
import com.pms.hotel.company.internal.web.CompanyRequests.CreateCompanyRequest;
import com.pms.hotel.company.internal.web.CompanyRequests.UpdateCompanyRequest;
import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService implements CompanyApi {

    private final CompanyRepository companyRepository;
    private final RatePlanApi ratePlanApi;

    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySummary getById(Long id) {
        return findEntity(id).toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanySummary> findAllActive() {
        return companyRepository.findByActiveTrue().stream().map(Company::toSummary).toList();
    }

    public Company create(CreateCompanyRequest request) {
        Company company = new Company();
        company.setName(request.name());
        company.setAddress(request.address());
        company.setContactEmail(request.contactEmail());
        company.setPhoneNumber(request.phoneNumber());
        applyNegotiatedRatePlan(company, request.negotiatedRatePlanId());
        if (request.billingCycle() != null) company.setBillingCycle(request.billingCycle());
        return companyRepository.save(company);
    }

    public Company update(Long id, UpdateCompanyRequest request) {
        Company company = findEntity(id);
        if (request.name() != null) company.setName(request.name());
        if (request.address() != null) company.setAddress(request.address());
        if (request.contactEmail() != null) company.setContactEmail(request.contactEmail());
        if (request.phoneNumber() != null) company.setPhoneNumber(request.phoneNumber());
        if (request.active() != null) company.setActive(request.active());
        if (request.negotiatedRatePlanId() != null) applyNegotiatedRatePlan(company, request.negotiatedRatePlanId());
        if (request.billingCycle() != null) company.setBillingCycle(request.billingCycle());
        return companyRepository.save(company);
    }

    private void applyNegotiatedRatePlan(Company company, Long negotiatedRatePlanId) {
        ratePlanApi.getById(negotiatedRatePlanId); // 404 si le tarif n'existe pas
        company.setNegotiatedRatePlanId(negotiatedRatePlanId);
    }

    public Company findEntity(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Entreprise", id));
    }
}
