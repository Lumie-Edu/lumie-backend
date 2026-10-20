@integration @BDD @ATDD @US2 @FR-006 @FR-007 @FR-008
Feature: Durable deletion converges for attachment lifecycle changes

  @BDD-US2-001 @ATDD-US2-001
  Scenario Outline: Commit delete intent for every supported deletion route
    Given a READY attachment is affected by <operation>
    When the database change commits
    Then the attachment is no longer readable
    And a durable delete intent exists for its permanent object

    @single-file
    Examples: Single-file deletion
      | operation |
      | single-file deletion |

    @announcement
    Examples: Announcement changes
      | operation |
      | announcement replacement |
      | announcement deletion |

    @textbook-folder
    Examples: Nested textbook-folder deletion
      | operation |
      | textbook-folder deletion |

  @BDD-US2-002 @ATDD-US2-002 @EC-002
  Scenario: Treat an already missing permanent object as deleted
    Given a durable delete intent for an object that is already absent
    When the delete processor handles the intent
    Then the intent reaches successful completion

  @BDD-US2-003 @ATDD-US2-003 @EC-004
  Scenario Outline: Reject retained attachment IDs outside the announcement
    Given an announcement replacement contains a retained attachment ID that is <ownership>
    When Staff submits the replacement
    Then the request is rejected before deleting any existing attachment
    And no object outside the announcement is modified
    Examples:
      | ownership |
      | missing |
      | owned by another tenant |
      | owned by another announcement |

  @BDD-US2-004 @ATDD-US2-004 @EC-005
  Scenario: Keep failed folder-object deletion independently retryable
    Given a nested folder has two READY files and MinIO fails to delete one object transiently
    When Staff deletes the folder and the delete processor handles both intents
    Then both file records are unavailable
    And the successful object deletion completes independently
    And the failed object's delete intent remains durable and retryable
