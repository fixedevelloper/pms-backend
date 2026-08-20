package com.pms.hotel.company.internal.web;

import com.pms.hotel.company.internal.Company;
import com.pms.hotel.company.internal.CompanyService;
import com.pms.hotel.company.internal.web.CompanyRequests.CreateCompanyRequest;
import com.pms.hotel.company.internal.web.CompanyRequests.UpdateCompanyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public List<Company> index() {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    public Company show(@PathVariable Long id) {
        return companyService.findEntity(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company store(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    public Company update(@PathVariable Long id, @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.update(id, request);
    }
}
