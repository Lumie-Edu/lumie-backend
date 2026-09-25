-- Remove the retired sidebar item before runtime validation stops accepting it.
update tenants
set hidden_sidebar_item_ids = coalesce(
    (
        select jsonb_agg(item)
        from jsonb_array_elements(hidden_sidebar_item_ids) as item
        where item <> '"spreadsheets"'::jsonb
    ),
    '[]'::jsonb
)
where hidden_sidebar_item_ids ? 'spreadsheets';
