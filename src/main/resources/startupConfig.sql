INSERT INTO scheme (id, name, regex) VALUES (1, 'MasterCard', '^5[1-5][0-9]{14}|^(222[1-9]|22[3-9]\\d|2[3-6]\\d{2}|27[0-1]\\d|2720)[0-9]{12}$')
INSERT INTO scheme (id, name, regex) VALUES (2, 'Visa', '^4[0-9]{12}(?:[0-9]{3})?$')
INSERT INTO scheme (id, name, regex) VALUES (3, 'Verve', '^3[0-9]{12}(?:[0-9]{3})?$')
INSERT INTO scheme (id, name, regex) VALUES (4, 'Amex', '^6[0-9]{12}(?:[0-9]{3})?$')
INSERT INTO scheme (id, name, regex) VALUES (5, 'Visa', '^4\d{15,18}$')
INSERT INTO scheme (id, name, regex) VALUES (6, 'MasterCard', '^6799\d{15,18}$')
INSERT INTO scheme (id, name, regex) VALUES (7, 'MasterCard', '^6799\d{0,100}$')


INSERT INTO terminals (id, actual_terminalname, amount_left, amount_paid, assigned_by, assigned_flag, assigned_to, date_created, date_updated, deleted, description, image, issued_date, number_of_payment_time, preferred_terminal_name, status, terminal_amount, terminal_id, terminal_name, terminal_serial_number, terminal_type, updated_by, user_id) VALUES (DEFAULT, '0', null, null, null, true, null, null, null, false, null, null, null, 1, null, 0, null, '2011338S', 'NEXGO', null, null, null, '1')

INSERT INTO routing_rule (id, deleted, maximum_amount, minimum_amount, precedence, scheme, type, user_id, values, station_id) VALUES (DEFAULT, false, null, null, 1, 'MasterCard', 'SCHEME', '1', null, 1)
INSERT INTO routing_rule (id, deleted, maximum_amount, minimum_amount, precedence, scheme, type, user_id, values, station_id) VALUES (DEFAULT, false, null, null, 2, 'Visa', 'SCHEME', '1', null, 1)
INSERT INTO routing_rule (id, deleted, maximum_amount, minimum_amount, precedence, scheme, type, user_id, values, station_id) VALUES (DEFAULT, false, null, null, 3, 'Verve', 'SCHEME', '1', null, 1)

INSERT INTO stations (id, last_echo, last_zpk_change, name, new_zpk_kcv, status, zmk, zmk_kcv, zpk, zpk_kcv) VALUES (DEFAULT, null, null, 'unifiedpayment', '3CDDE1CC6FDD225C9A8BC3EB065509A6', null, null, null, null, null)
INSERT INTO stations (id, last_echo, last_zpk_change, name, new_zpk_kcv, status, zmk, zmk_kcv, zpk, zpk_kcv) VALUES (DEFAULT, null, null, 'iswagencybanking', '11111111111111111111111111111111', null, null, null, null, null)
INSERT INTO stations (id, last_echo, last_zpk_change, name, new_zpk_kcv, status, zmk, zmk_kcv, zpk, zpk_kcv) VALUES (DEFAULT, null, null, 'nibss', 'DBEECACCB4210977ACE73A1D873CA59F', null, null, null, null, null)
