package com.blackbox.project.dto;

public class ConsentRequest {

    private Boolean consentPlatform;
    private Boolean consentGithub;
    private Boolean consentDrive;
    private Boolean consentAiAnalysis;

    public ConsentRequest() {}

    public Boolean getConsentPlatform() { return consentPlatform; }
    public void setConsentPlatform(Boolean consentPlatform) { this.consentPlatform = consentPlatform; }
    public Boolean getConsentGithub() { return consentGithub; }
    public void setConsentGithub(Boolean consentGithub) { this.consentGithub = consentGithub; }
    public Boolean getConsentDrive() { return consentDrive; }
    public void setConsentDrive(Boolean consentDrive) { this.consentDrive = consentDrive; }
    public Boolean getConsentAiAnalysis() { return consentAiAnalysis; }
    public void setConsentAiAnalysis(Boolean consentAiAnalysis) { this.consentAiAnalysis = consentAiAnalysis; }
}
