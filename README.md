# Tickr: An event management System

## How to start Tickr

### Frontend
Move into frontend directory using the following command
```
  cd frontend
```

Once in the frontend directory paste the following command to install the required modules.
This is required on the first start up of the front end
```
  npm install -f
```

To start the frontend run the following command
```
  npm start
```

### Backend
In order to start the backend, in the root directory simply run
```
start-server.sh
```
This will initialise the database, various APIs and start the server.

#### Dummy data
If you wish to make use of the dummy data generation script, first `cd` to the `backend` directory and run
```
./backend.sh
```
This runs the backend with empty email and payment APIs suitable for data generation. After that, in a 
second shell run in the root directory
```
python3 dummy_data.py <data.json>
```
which will clear the database and populate with the required data. Afterwards, simply run `./start-server.sh`
in the root directory to use the server as normal.  
`dummy_data.json` and `demo_data.json` have been provided if you are interested.

#### Emails
The email API implementation will automatically intercept any emails with an @example.com domain and prevent those emails from being sent. If you are not intending to test the email sending feature for an account, consider using such a domain to ensure that the email API rate limit is not exceeded.

#### Database management
If you wish to restart the database, run
```
./db-restart.sh
```
which will close and re-open the database, **resetting all data**. When you wish to shutdown the database, `cd` to the `backend` directory and run `./db-shutdown.sh`, or run `backend/db-shutdown.sh` from the project root directory.

