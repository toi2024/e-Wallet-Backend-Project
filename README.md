# e-Wallet-Backend-Project
E-wallet system that has high volume transaction processing

#Note 
When running the project you will encounter "Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections. HHH000342: Could not obtain connection to query metadata"
This error means PostgreSQL is not running or not accessible on port 5432

#To solve this 

Solution 1: Start PostgreSQL (If Already Installed)

<img width="550" height="317" alt="image" src="https://github.com/user-attachments/assets/5575351e-c991-4ff3-8ae3-546bdf7eff37" />

Solution 2: Install PostgreSQL Using Docker (Recommended for Development)
This is the easiest and cleanest approach for development

Step 1: Install Docker

Windows/Mac: Download from https://www.docker.com/products/docker-desktop
Linux:
<img width="728" height="157" alt="image" src="https://github.com/user-attachments/assets/bf257bdb-5207-4f36-bf33-931ee8c1b4c2" />

Step 2: Run PostgreSQL Container 
<img width="718" height="356" alt="image" src="https://github.com/user-attachments/assets/149b139b-9919-43e0-8c52-a16a5962dee6" />

Step 3: Create Database Schema
<img width="747" height="224" alt="image" src="https://github.com/user-attachments/assets/6d0e61c6-2876-418c-a0f3-5f68b4eac543" />
<img width="735" height="358" alt="image" src="https://github.com/user-attachments/assets/f40c33ef-eda4-4f49-bda0-69c78feb69c4" />



