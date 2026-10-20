@integration @BDD @ATDD @US3 @FR-008 @FR-010 @FR-011 @FR-012 @FR-013
Feature: Storage operation retries remain bounded and observable

  @BDD-US3-001 @ATDD-US3-001 @EC-006
  Scenario: Escalate a promotion that cannot finish before temporary expiry
    Given a PENDING attachment nearing its 24-hour temporary-object expiry
    When promotion has not succeeded by its bounded retry deadline
    Then the attachment remains unavailable
    And an actionable terminal failure alert is emitted

  @BDD-US3-002 @ATDD-US3-002 @EC-003
  Scenario: A stale completion cannot undo a later deletion
    Given a promotion operation completed and a later delete made its attachment unavailable
    When the processor receives the completed promotion operation again
    Then the attachment remains unavailable
    And the durable delete intent remains processable until the permanent object is absent

  @ATDD-US3-003 @EC-007
  Scenario: Keep attachment data and storage work inside its tenant boundary
    Given a PENDING attachment and its durable storage operation belong to Tenant A
    When a Staff member from Tenant B requests the attachment and a Tenant B processor claims due work
    Then the Staff member cannot read or download Tenant A's attachment
    And the processor cannot observe or claim Tenant A's operation
    And no storage action is performed on Tenant A's object key
    When a Tenant A processor claims due work
    Then it can process only Tenant A's operation using Tenant A's context

  @BDD-US3-004 @ATDD-US3-004 @FR-011 @FR-012
  Scenario: Preserve and alert on a terminal permanent-object deletion failure
    Given a delete operation has reached its retry deadline after transient storage failures
    When the processor marks the operation terminally failed
    Then the attachment remains unavailable
    And the durable delete intent and last failure remain auditable
    And the terminal-deletion alert signal is emitted

  @BDD-US3-005 @ATDD-US3-005 @FR-011
  Scenario: Warn operators about a stale retry backlog
    Given a storage operation remains pending or retrying for more than five minutes
    When the monitoring rule evaluates the operation metrics
    Then the stale-backlog warning fires
    And the operation can be identified from its log details without tenant or object labels on metrics
