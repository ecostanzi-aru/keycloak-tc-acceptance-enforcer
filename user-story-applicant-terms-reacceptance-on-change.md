# Applicant accepts changed Terms and Conditions


**As a**

applicant

**I want to**

review and accept Terms and Conditions again when they have changed

**So that**

I can make an informed decision before accessing the service.

**Acceptance criteria:**

1. **GIVEN** Terms and Conditions have been configured  
   **AND** I am registering for the first time  
   **WHEN** I reach the Terms and Conditions step of registration  
   **THEN** I see the configured Terms and Conditions  
   **AND** I must accept them before I can complete registration.

2. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted the current Terms and Conditions  
   **WHEN** I log in again and the Terms and Conditions content has not changed  
   **THEN** I do not see the Terms and Conditions screen  
   **AND** I can continue with my login.

3. **GIVEN** Terms and Conditions have been configured  
   **AND** I have previously accepted them  
   **AND** the Terms and Conditions content has changed since my acceptance  
   **WHEN** I next register or log in  
   **THEN** I see the changed Terms and Conditions  
   **AND** I must accept them before I can continue.

4. **GIVEN** I am shown the Terms and Conditions screen during registration or login  
   **WHEN** I decline the Terms and Conditions  
   **THEN** I do not complete the registration or login flow  
   **AND** I am not recorded as having accepted the Terms and Conditions.
