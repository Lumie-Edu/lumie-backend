@BDD @ATDD @US1
Feature: Review answer correction history
  Staff can inspect completed corrections, including changes made before any results existed.

  @BDD-US1-003 @ATDD-US1-003 @FR-009 @SC-004
  Scenario: History shows actor, time, changed fields, and counts
    Given an authorized instructor is editing exam 5
    When the instructor reads correction history
    Then the history shows the recorded correction details

  @BDD-US1-008 @ATDD-US1-008 @EC-007
  Scenario: Correct an exam before results exist
    Given an authorized instructor is editing exam 5
    When the instructor confirms a correction for an exam without results
    Then the receipt records zero regraded results and submissions
