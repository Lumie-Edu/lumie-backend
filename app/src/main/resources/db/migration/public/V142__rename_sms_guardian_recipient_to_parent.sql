-- Canonicalize the SMS recipient value from GUARDIAN to PARENT.
-- Existing recipient history stays intact; only the role discriminator changes.
UPDATE sms_messages
SET recipient_type = 'PARENT'
WHERE recipient_type = 'GUARDIAN';

UPDATE sms_messages AS message
SET recipients = (
    SELECT jsonb_agg(
        CASE
            WHEN item ->> 'recipientRole' = 'GUARDIAN'
                THEN jsonb_set(item, '{recipientRole}', '"PARENT"'::jsonb)
            ELSE item
        END
        ORDER BY ordinal
    )
    FROM jsonb_array_elements(message.recipients) WITH ORDINALITY AS entry(item, ordinal)
)
WHERE message.recipients @> '[{"recipientRole":"GUARDIAN"}]'::jsonb;

UPDATE sms_recipient_groups AS recipient_group
SET recipients = (
    SELECT jsonb_agg(
        CASE
            WHEN item ->> 'recipientRole' = 'GUARDIAN'
                THEN jsonb_set(item, '{recipientRole}', '"PARENT"'::jsonb)
            ELSE item
        END
        ORDER BY ordinal
    )
    FROM jsonb_array_elements(recipient_group.recipients) WITH ORDINALITY AS entry(item, ordinal)
)
WHERE recipient_group.recipients @> '[{"recipientRole":"GUARDIAN"}]'::jsonb;
