MERGE INTO user_account (id, username, password, role, enabled) KEY(username) VALUES
(1, 'admin', '$2a$10$0x7R31X7j4WDYwVzmIvzde.UktFmpGGmJrtV9BcYsbZTxj2DWnOPS', 'ROLE_ADMIN', 1);
