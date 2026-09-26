INSERT INTO settings (setting_key, setting_value, description, updated_at) VALUES
    ('offer_hours', '48', 'Hours a bed offer stays open before it expires (BR-05)', UTC_TIMESTAMP(6)),
    ('warning_threshold', '3', 'Violations in one term that create an eviction proposal (BR-11)', UTC_TIMESTAMP(6)),
    ('repair_due_hours_urgent', '24', 'Repair deadline for urgent tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
    ('repair_due_hours_normal', '72', 'Repair deadline for normal tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
    ('repair_due_hours_low', '168', 'Repair deadline for low priority tickets, in hours (BR-10)', UTC_TIMESTAMP(6));
