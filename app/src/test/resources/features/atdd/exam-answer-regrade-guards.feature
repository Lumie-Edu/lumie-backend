@BDD @ATDD @US1
Feature: Do not race answer corrections with active batch OMR grading
  Staff must wait until OMR grading is complete before changing the answer key.

  @BDD-US1-013 @ATDD-US1-013 @FR-013 @EC-009 @SC-003
  Scenario: An active OMR batch blocks answer-correction preview
    Given an authorized instructor has a pending OMR job for this exam
    When the instructor previews changing question 44 to answer "4"
    Then the correction is blocked until the OMR job finishes

  @BDD-US1-009 @ATDD-US1-009 @FR-010 @EC-008
  Scenario: A staff session cannot use correction routes addressed to another Tenant
    Given an authorized instructor is reviewing an exam with existing results
    When the instructor requests correction routes under another Tenant host binding
    Then the cross-Tenant request is denied before regrade logic runs

  @BDD-US1-004 @ATDD-US1-004 @EC-001 @SC-003
  Scenario: A concurrent correction is rejected
    Given an authorized instructor is editing exam 5
    And another correction conflicts with this one
    When the instructor confirms the correction with an idempotency key
    Then the correction reports a conflict

  @BDD-US1-005 @ATDD-US1-005 @EC-002 @FR-012
  Scenario: Incomplete saved question data is rejected
    Given an authorized instructor is editing exam 5
    And saved question data is incomplete
    When the instructor confirms the correction with an idempotency key
    Then the correction reports invalid answer data

  @BDD-US1-006 @ATDD-US1-006 @EC-004 @SC-005
  Scenario: A manually overridden result is counted as skipped
    Given an authorized instructor is editing exam 5
    And the exam includes two manually overridden results
    When the instructor confirms the correction with an idempotency key
    Then the receipt reports two skipped manual overrides

  @BDD-US1-007 @ATDD-US1-007 @EC-006 @FR-011
  Scenario: Unchanged exam information creates no revision
    Given an authorized instructor is editing exam 5
    And the submitted exam information is unchanged
    When the instructor confirms the correction with an idempotency key
    Then the receipt reports no change and no revision

  @BDD-US1-010:idempotency-conflict @ATDD-US1-010:idempotency-conflict @EC-005
  Scenario: Reusing an idempotency key for a different correction is rejected
    Given an authorized instructor is editing exam 5
    When the instructor confirms two different corrections with one idempotency key
    Then only the first correction is applied and the second conflicts

  @BDD-US1-010:unauthorized @ATDD-US1-010:unauthorized @FR-008
  Scenario: A student cannot confirm an answer correction
    Given a student is editing exam 5
    When the student confirms the correction
    Then the correction is denied before regrade logic runs

  @BDD-US1-011 @ATDD-US1-011 @EC-003
  Scenario: Completed unmatched OMR grading is included in the receipt
    Given an authorized instructor is editing exam 5
    And a completed unmatched OMR submission is affected
    When the instructor confirms the correction with an idempotency key
    Then the receipt counts the updated OMR submission
