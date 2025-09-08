-- Test data for Secure QR Voting System
-- This file is loaded automatically by Spring Boot for testing

-- Insert test users
INSERT INTO users (user_id, username, email, password_hash, qr_code_secret, role) VALUES
('admin-001', 'admin', 'admin@securevoting.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrAqjvxS9Vy', 'ADMIN_QR_SECRET_2024', 'ADMIN'),
('voter-001', 'alice', 'alice@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrAqjvxS9Vy', 'ALICE_QR_SECRET_2024', 'VOTER'),
('voter-002', 'bob', 'bob@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrAqjvxS9Vy', 'BOB_QR_SECRET_2024', 'VOTER'),
('voter-003', 'charlie', 'charlie@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrAqjvxS9Vy', 'CHARLIE_QR_SECRET_2024', 'VOTER'),
('voter-004', 'diana', 'diana@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrAqjvxS9Vy', 'DIANA_QR_SECRET_2024', 'VOTER');

-- Insert test elections
INSERT INTO elections (election_id, title, description, start_time, end_time, status, created_by) VALUES
('election-001', 'Student Council President 2024', 'Annual election for student council president position', 
 DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 7, CURRENT_TIMESTAMP), 'ACTIVE', 'admin-001'),
('election-002', 'Best Programming Language', 'Community vote for the most preferred programming language',
 DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('DAY', 14, CURRENT_TIMESTAMP), 'ACTIVE', 'admin-001'),
('election-003', 'Future Technology Focus', 'Vote on which technology area to focus on next quarter',
 DATEADD('DAY', 1, CURRENT_TIMESTAMP), DATEADD('DAY', 21, CURRENT_TIMESTAMP), 'PENDING', 'admin-001');

-- Insert test candidates
INSERT INTO candidates (candidate_id, name, description, election_id) VALUES
-- Student Council President candidates
('candidate-001', 'Sarah Johnson', 'Computer Science major with leadership experience in debate club', 'election-001'),
('candidate-002', 'Michael Chen', 'Engineering student focused on sustainability and innovation', 'election-001'),
('candidate-003', 'Emma Rodriguez', 'Business major with experience in student organizations', 'election-001'),

-- Programming Language candidates
('candidate-004', 'Java', 'Enterprise-grade, object-oriented programming language', 'election-002'),
('candidate-005', 'Python', 'Versatile, readable language great for data science and web development', 'election-002'),
('candidate-006', 'JavaScript', 'Dynamic language essential for web development', 'election-002'),
('candidate-007', 'Go', 'Modern language designed for scalability and performance', 'election-002'),

-- Technology Focus candidates
('candidate-008', 'Artificial Intelligence', 'Focus on AI and machine learning technologies', 'election-003'),
('candidate-009', 'Blockchain', 'Explore distributed ledger and cryptocurrency technologies', 'election-003'),
('candidate-010', 'Cloud Computing', 'Advance cloud infrastructure and serverless technologies', 'election-003'),
('candidate-011', 'Cybersecurity', 'Strengthen security practices and threat detection', 'election-003');

-- Insert sample audit logs
INSERT INTO audit_logs (log_id, user_id, action, details, ip_address, event_type) VALUES
('audit-001', 'admin-001', 'LOGIN', 'Admin user logged in successfully', '127.0.0.1', 'AUTHENTICATION'),
('audit-002', 'admin-001', 'CREATE_ELECTION', 'Created election: Student Council President 2024', '127.0.0.1', 'ADMIN_ACTION'),
('audit-003', 'voter-001', 'LOGIN', 'User logged in successfully', '127.0.0.1', 'AUTHENTICATION'),
('audit-004', 'voter-002', 'LOGIN', 'User logged in successfully', '127.0.0.1', 'AUTHENTICATION'),
('audit-005', 'admin-001', 'CREATE_ELECTION', 'Created election: Best Programming Language', '127.0.0.1', 'ADMIN_ACTION');