@BDD @ATDD @US1 @FR-002 @FR-003
Feature: Preview an exam answer correction
  Staff review score impact before saving an answer-key change.

  @BDD-US1-001 @ATDD-US1-001 @SC-001
  Scenario: Preview impact without changing saved data
    Given an authorized instructor is reviewing an exam with existing results
    When the instructor previews changing question 44 to answer "4"
    Then the preview is returned without changing saved exam data
    And the preview request uses the authenticated instructor session
