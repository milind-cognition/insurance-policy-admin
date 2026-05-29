package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "POLICY_HOLDERS", schema = "ACMEINS")
public class PolicyHolder {

    public static final String RISK_TIER_PREFERRED = "P";
    public static final String RISK_TIER_STANDARD = "S";
    public static final String RISK_TIER_SUBSTANDARD = "U";

    @Id
    @Column(name = "CUST_ID", length = 10, nullable = false)
    private String custId;

    @Column(name = "CUST_TYPE", length = 1, nullable = false)
    private String custType = "I";

    @Column(name = "LAST_NAME", length = 30)
    private String lastName;

    @Column(name = "FIRST_NAME", length = 20)
    private String firstName;

    @Column(name = "MIDDLE_INIT", length = 1)
    private String middleInit;

    @Column(name = "COMPANY_NAME", length = 50)
    private String companyName;

    @Column(name = "ADDR_LINE1", length = 40)
    private String addrLine1;

    @Column(name = "ADDR_LINE2", length = 40)
    private String addrLine2;

    @Column(name = "CITY", length = 25)
    private String city;

    @Column(name = "STATE_CODE", length = 2)
    private String stateCode;

    @Column(name = "ZIP_CODE", length = 10)
    private String zipCode;

    @Column(name = "COUNTRY_CODE", length = 3)
    private String countryCode = "USA";

    @Column(name = "PHONE", length = 15)
    private String phone;

    @Column(name = "EMAIL", length = 60)
    private String email;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "SSN_LAST4", length = 4)
    private String ssnLast4;

    @Column(name = "TAX_ID", length = 10)
    private String taxId;

    @Column(name = "CREDIT_SCORE")
    private Integer creditScore;

    @Column(name = "RISK_TIER", length = 1)
    private String riskTier = RISK_TIER_STANDARD;

    @Column(name = "GDPR_CONSENT", length = 1)
    private String gdprConsent = "N";

    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDate createdDate;

    @Column(name = "LAST_UPDATED", nullable = false)
    private LocalDateTime lastUpdated;

    public PolicyHolder() {
    }

    public String getCustId() {
        return custId;
    }

    public void setCustId(String custId) {
        this.custId = custId;
    }

    public String getCustType() {
        return custType;
    }

    public void setCustType(String custType) {
        this.custType = custType;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleInit() {
        return middleInit;
    }

    public void setMiddleInit(String middleInit) {
        this.middleInit = middleInit;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddrLine1() {
        return addrLine1;
    }

    public void setAddrLine1(String addrLine1) {
        this.addrLine1 = addrLine1;
    }

    public String getAddrLine2() {
        return addrLine2;
    }

    public void setAddrLine2(String addrLine2) {
        this.addrLine2 = addrLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSsnLast4() {
        return ssnLast4;
    }

    public void setSsnLast4(String ssnLast4) {
        this.ssnLast4 = ssnLast4;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public int getCreditScore() {
        return creditScore != null ? creditScore : 0;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(String riskTier) {
        this.riskTier = riskTier;
    }

    public String getGdprConsent() {
        return gdprConsent;
    }

    public void setGdprConsent(String gdprConsent) {
        this.gdprConsent = gdprConsent;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
