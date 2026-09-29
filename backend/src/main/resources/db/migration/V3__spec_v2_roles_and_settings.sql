-- Spec v2 removes the AFFAIRS role. Existing accounts become building managers with no building
-- (not deleted: audit_logs.user_id may reference them).
UPDATE users SET role = 'BUILDING_MANAGER' WHERE role = 'AFFAIRS';

-- Spec v2 settings (CLAUDE.md "Settings and their defaults") replace every Phase 0 key.
DELETE FROM settings;
INSERT INTO settings (setting_key, setting_value, description, updated_at) VALUES
    ('water_fee_monthly', '40000', 'Water fee per month in VND, part of the semester fee (BR-12)', UTC_TIMESTAMP(6)),
    ('equipment_fee', '300000', 'Equipment fee in VND, charged once in FIRST_TIME rounds (BR-12)', UTC_TIMESTAMP(6)),
    ('electricity_price_per_kwh', '3000', 'Electricity price in VND per kWh (BR-07)', UTC_TIMESTAMP(6)),
    ('default_hold_minutes', '30', 'Hold time in minutes pre-filled for a new registration round (BR-05)', UTC_TIMESTAMP(6)),
    ('overdue_days_block_stay_on', '30', 'Days after issue after which an unpaid electricity invoice blocks stay-on (BR-09)', UTC_TIMESTAMP(6)),
    ('repair_deadline_hours_urgent', '24', 'Repair deadline for urgent tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
    ('repair_deadline_hours_normal', '72', 'Repair deadline for normal tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
    ('repair_deadline_hours_low', '168', 'Repair deadline for low priority tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
    ('warning_threshold', '3', 'Violations in one term that create a warning review for the Centre Administrator (BR-11)', UTC_TIMESTAMP(6));
