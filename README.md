
# Organ Donor Matching System

A Java application that registers organ donors and recipients, keeps a prioritized waiting list, and suggests the best recipient for each available donor. It has a console menu and a Swing graphical interface.

> ## ⚠️ Disclaimer
>
> This project was developed for **educational purposes** as part of a college course. It demonstrates donor-recipient matching logic and is **not a medical system**.
>
> - All names, blood groups, organs and other records in the sample data are **fictional**.
> - The matching rules are **simplified**. Only blood group compatibility, organ type, urgency and waiting time are considered. Real transplant programs also consider tissue (HLA) typing, organ size, distance and transport time, and clinical assessment.
> - The rankings shown by this application **must not be used for any real medical decision**.
> - In real life, organ allocation is carried out by authorized medical professionals and official bodies, such as NOTTO and state transplant organizations in India.
> - **Do not enter real personal or medical data** into this application.

## Features

- Register donors and recipients, with validation (age limits, empty names, duplicate entries)
- Waiting list with filtering by organ
- Match finder: ranks compatible recipients for a chosen donor by urgency, then by waiting time
- The coordinator confirms the final match; the ranking is only a suggestion
- Match history and summary reports (counts by organ, average waiting time)
- Two interfaces: console menu (`Main.java`) and Swing GUI (`MainGUI.java`)

## How matching works

1. A recipient is a candidate only if the **organ** matches and the **blood group** is compatible with the donor's.
2. Candidates are ranked by **urgency** (1 to 5, where 5 is most urgent).
3. Ties are broken by **days waiting**, so longer waits rank higher.
4. The user reviews the ranked list and confirms one match. Confirming marks both the donor and the recipient as matched and records the match in the history.

## Limitations

- Does not model HLA tissue typing, organ size, geography or transport time, or clinical eligibility.
- No user accounts or login. Anyone running the application can confirm matches.
- Intended for demonstration with sample data, not for production use.

## Tech stack

![Java](https://img.shields.io/badge/Java-7986CB?style=for-the-badge&logo=openjdk&logoColor=white)
![Java Swing](https://img.shields.io/badge/Java_Swing-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-29B6F6?style=for-the-badge&logo=mysql&logoColor=white)
## Project structure

```
organ-donor-matching/
├── .vscode/                      editor settings
├── lib/
│   └── mysql-connector-j.jar     MySQL JDBC driver
├── sql/
│   ├── schema.sql                creates the tables
│   └── sample_data.sql           fictional sample records
├── src/
│   ├── Main.java                 console menu
│   ├── MainGUI.java              launches the Swing GUI
│   ├── ModernGUI.java            Swing interface (sidebar layout)
│   ├── MatchService.java         matching rules and business logic
│   ├── DBConnection.java         reads config.properties, opens connections
│   ├── Donor.java                model
│   ├── Recipient.java            model
│   ├── MatchRecord.java          model
│   ├── DonorDAO.java             donor database access
│   ├── RecipientDAO.java         recipient database access
│   ├── MatchDAO.java             match database access
│   ├── TestConnection.java       checks the database connection
│   ├── TestDonorDAO.java         manual test for DonorDAO
│   └── TestMatchDAO.java         manual test for MatchDAO
├── .gitignore
├── config.example.properties     template for database settings
└── README.md
```

## Setup

1. Install the JDK and MySQL, and make sure the MySQL service is running.
2. Create the database and tables by running `schema.sql`, then load the sample records with `sample_data.sql`.
3. Create `config.properties` in the project root with your own database settings (URL, user, password). **Never commit this file.** It should be listed in `.gitignore`.
4. Add the MySQL JDBC driver to the project's libraries.

## Running

- Graphical interface: run `src/MainGUI.java`
- Console version: run `src/Main.java`

Open the project from its root folder so `config.properties` is found. Confirming a match changes the stored data, so re-run `schema.sql` and `sample_data.sql` to reset the sample data after testing.

## Authors

- Nivedha T [250701503]
- Poshitha S [250701526]
