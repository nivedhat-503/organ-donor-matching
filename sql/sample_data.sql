
USE organ_matching;

INSERT INTO donors (donor_id, name, age, blood_group, organ_type, city, status, registered_date) VALUES
(1,  'Arjun Krishnan',       34, 'O-',  'Kidney',   'Chennai',    'AVAILABLE', '2026-09-02'),  
(2,  'Priya Venkatesh',      28, 'O+',  'Kidney',   'Coimbatore', 'AVAILABLE', '2026-08-18'),
(3,  'Rajesh Sharma',        45, 'A+',  'Liver',    'Mumbai',     'AVAILABLE', '2026-09-10'),
(4,  'Fatima Sheikh',        39, 'B+',  'Kidney',   'Hyderabad',  'AVAILABLE', '2026-07-25'),
(5,  'Karthik Subramanian',  52, 'AB+', 'Kidney',   'Chennai',    'AVAILABLE', '2026-09-15'), 
(6,  'Meenakshi Iyer',       31, 'A-',  'Liver',    'Bengaluru',  'AVAILABLE', '2026-08-30'),
(7,  'Sandeep Reddy',        41, 'B-',  'Heart',    'Hyderabad',  'AVAILABLE', '2026-09-20'),
(8,  'Ananya Das',           26, 'O+',  'Lung',     'Kolkata',    'AVAILABLE', '2026-09-05'),
(9,  'Gurpreet Singh',       58, 'A+',  'Kidney',   'Delhi',      'AVAILABLE', '2026-08-11'),
(10, 'Lakshmi Narayanan',    47, 'O+',  'Liver',    'Madurai',    'AVAILABLE', '2026-07-30'),
(11, 'Vikram Joshi',         36, 'AB-', 'Kidney',   'Pune',       'AVAILABLE', '2026-09-12'),  
(12, 'Divya Nair',           29, 'B+',  'Pancreas', 'Kochi',      'AVAILABLE', '2026-09-18'),  
(13, 'Mohammed Irfan',       33, 'O-',  'Heart',    'Bengaluru',  'AVAILABLE', '2026-09-08'),
(14, 'Harini Balaji',        24, 'A+',  'Kidney',   'Trichy',     'AVAILABLE', '2026-09-22'), 
(15, 'Sunil Patil',          61, 'B+',  'Liver',    'Pune',       'AVAILABLE', '2026-08-04'),
(16, 'Nandini Rao',          38, 'O+',  'Kidney',   'Chennai',    'MATCHED',   '2026-05-10'),
(17, 'Anil Kumar Yadav',     50, 'A+',  'Liver',    'Delhi',      'MATCHED',   '2026-06-01');


INSERT INTO recipients (recipient_id, name, age, blood_group, organ_needed, city, urgency, status, registered_date) VALUES
-- Kidney
(1,  'Suresh Babu',        42, 'A+',  'Kidney',   'Chennai',    4, 'WAITING', '2025-03-10'),
(2,  'Kavitha Rajan',      36, 'O+',  'Kidney',   'Chennai',    4, 'WAITING', '2024-11-05'),  
(3,  'Imran Khan',         55, 'B+',  'Kidney',   'Hyderabad',  5, 'WAITING', '2026-02-14'),
(4,  'Deepa Menon',        47, 'AB+', 'Kidney',   'Kochi',      3, 'WAITING', '2025-08-20'),
(5,  'Rohit Verma',        29, 'O+',  'Kidney',   'Delhi',      2, 'WAITING', '2025-01-12'),
(6,  'Shalini Gupta',      33, 'A-',  'Kidney',   'Pune',       3, 'WAITING', '2025-06-30'),
(7,  'Balaji Murugan',     60, 'AB-', 'Kidney',   'Coimbatore', 5, 'WAITING', '2025-12-01'),  
(8,  'Revathi Sundaram',   44, 'B-',  'Kidney',   'Madurai',    3, 'WAITING', '2025-05-05'),
(9,  'Pooja Chandran',     31, 'A+',  'Kidney',   'Trichy',     3, 'WAITING', '2025-04-18'),  
(10, 'Naveen Prasad',      38, 'A+',  'Kidney',   'Salem',      3, 'WAITING', '2025-04-18'),  
-- Liver
(11, 'Anita Deshmukh',     50, 'A+',  'Liver',    'Mumbai',     4, 'WAITING', '2025-05-22'),
(12, 'Ramesh Pillai',      58, 'O+',  'Liver',    'Chennai',    5, 'WAITING', '2026-01-09'),
(13, 'Sneha Kulkarni',     27, 'B+',  'Liver',    'Pune',       3, 'WAITING', '2025-09-15'),
(14, 'Arvind Swamy',       49, 'AB+', 'Liver',    'Bengaluru',  2, 'WAITING', '2025-10-30'),
(15, 'Zainab Ali',         41, 'A+',  'Liver',    'Hyderabad',  4, 'WAITING', '2025-02-03'),
-- Heart
(16, 'Gopal Krishnan',     52, 'B+',  'Heart',    'Hyderabad',  5, 'WAITING', '2026-03-12'),
(17, 'Manisha Tiwari',     45, 'O+',  'Heart',    'Delhi',      4, 'WAITING', '2025-11-18'),
-- Lung
(18, 'Debashish Banerjee', 57, 'A+',  'Lung',     'Kolkata',    5, 'WAITING', '2025-07-07'),
(19, 'Tanvi Shah',         34, 'O+',  'Lung',     'Mumbai',     3, 'WAITING', '2025-04-01'),
-- Pancreas 
(20, 'Rahul Mehta',        30, 'O+',  'Pancreas', 'Ahmedabad',  4, 'WAITING', '2025-09-09'),
-- Already matched
(21, 'Seema Rao',          35, 'O+',  'Kidney',   'Chennai',    5, 'MATCHED', '2025-01-20'),
(22, 'Harish Chand',       46, 'A+',  'Liver',    'Delhi',      4, 'MATCHED', '2025-02-11');



INSERT INTO matches (match_id, donor_id, recipient_id, match_date) VALUES
(1, 16, 21, '2026-06-15 10:30:00'),
(2, 17, 22, '2026-07-02 14:15:00');


