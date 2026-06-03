Feature: Policy inquiry through the PAS facade
  As a downstream system integrator
  I want to query policy and coverage data from the mainframe via the REST facade
  So that I can service customers without direct DB2/CICS access

  Background:
    Given the PAS facade is available

  Scenario: Look up an active homeowners policy
    When I request policy "POL-00000001"
    Then the response status is 200
    And the policy type is "HOM"
    And the policy status is "AC"
    And the total premium is 1250.00

  Scenario: Retrieve the coverages on a policy
    When I request the coverages for policy "POL-00000001"
    Then the response status is 200
    And there are 2 coverages
    And the coverage types are "DWEL, PERS"

  Scenario: Inquiry for a policy that does not exist
    When I request policy "POL-DOES-NOT-EXIST"
    Then the response status is 404

  Scenario Outline: Seeded mainframe policies are reachable by line of business
    When I request policy "<policyNumber>"
    Then the response status is 200
    And the policy type is "<policyType>"

    Examples:
      | policyNumber  | policyType |
      | POL-00000001  | HOM        |
      | POL-00000002  | AUT        |
      | POL-00000003  | CGL        |
