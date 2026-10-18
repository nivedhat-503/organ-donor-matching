CREATE DATABASE IF NOT EXISTS organ_matching;
USE organ_matching;

CREATE TABLE donors (
    donor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL CHECK (age BETWEEN 18 AND 65),
    blood_group VARCHAR(3) NOT NULL,
    organ_type VARCHAR(30) NOT NULL,
    city VARCHAR(50) NOT NULL,
    status ENUM('AVAILABLE','MATCHED') NOT NULL DEFAULT 'AVAILABLE',
    registered_date DATE NOT NULL DEFAULT (CURRENT_DATE)
);

CREATE TABLE recipients (
    recipient_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    organ_needed VARCHAR(30) NOT NULL,
    city VARCHAR(50) NOT NULL,
    urgency TINYINT NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    status ENUM('WAITING','MATCHED') NOT NULL DEFAULT 'WAITING',
    registered_date DATE NOT NULL DEFAULT (CURRENT_DATE)
);

CREATE TABLE blood_compatibility (
    donor_group VARCHAR(3) NOT NULL,
    recipient_group VARCHAR(3) NOT NULL,
    PRIMARY KEY (donor_group, recipient_group)
);

CREATE TABLE matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    donor_id INT NOT NULL UNIQUE,
    recipient_id INT NOT NULL UNIQUE,
    match_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(donor_id),
    FOREIGN KEY (recipient_id) REFERENCES recipients(recipient_id)
);

INSERT INTO blood_compatibility (donor_group, recipient_group) VALUES
('O-','O-'),('O-','O+'),('O-','A-'),('O-','A+'),('O-','B-'),('O-','B+'),('O-','AB-'),('O-','AB+'),
('O+','O+'),('O+','A+'),('O+','B+'),('O+','AB+'),
('A-','A-'),('A-','A+'),('A-','AB-'),('A-','AB+'),
('A+','A+'),('A+','AB+'),
('B-','B-'),('B-','B+'),('B-','AB-'),('B-','AB+'),
('B+','B+'),('B+','AB+'),
('AB-','AB-'),('AB-','AB+'),
('AB+','AB+');