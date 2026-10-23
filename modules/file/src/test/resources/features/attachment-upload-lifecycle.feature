@integration @BDD @ATDD @US1 @FR-002 @FR-003 @FR-005
Feature: Register attachments as pending and hide them until usable

  @BDD-US1-001 @ATDD-US1-001
  Scenario: Keep a new attachment hidden while promotion is pending
    Given a complete temporary object with valid declared metadata
    When a Staff member registers the attachment
    Then the attachment is PENDING and is not listed or downloadable

  @BDD-US1-002 @ATDD-US1-002 @EC-001
  Scenario Outline: Do not expose an invalid or expired temporary object
    Given a PENDING attachment whose temporary object is <condition>
    When promotion is attempted
    Then the attachment does not become READY
    And the failure is recorded for retry or terminal handling
    Examples:
      | condition |
      | missing |
      | expired |
      | size-mismatched |
      | metadata-mismatched |

  @BDD-US1-003 @ATDD-US1-003 @FR-005
  Scenario: Expose an attachment only after permanent-object verification
    Given a PENDING attachment with a valid temporary object
    When the permanent object is promoted and its metadata is verified
    Then the attachment becomes READY
    And the attachment is listed and downloadable
