//This is the c++ file that contains what our header file functions do and how to use them for our db in SQLite

#include "storage.h"
#include <iostream>
using namespace std;

//Constructor
Storage::Storage() {
    db = nullptr;
    initialized = false;
}

//Destructor 
Storage::~Storage() {
    if(db != nullptr)
        sqlite3_close(db);
}

//Initializing the database
bool Storage::initiailize(){
    //opening or creating the database
    int result = sqlite3_open("Health.db", &db);
    //checking to see if opened successfully
    if(result != SQLITE_OK){
        cerr << "Database error: "
             << sqlite3_errmsg(db) << std::endl;
        return false;
    }

    char* error_message = nullptr;
    //Creating all 5 tables if they do not already exist: users, fitness, nutrition, sleep, and goals
    const char* sql = R"(
        Pragma foreign_keys = ON;
        
        CREATE TABLE IF NOT EXISTS Users(
            user_id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL
        )
            
        CREATE TABLE IF NOT EXISTS Fitness(
            fitness_id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            entry_date TEXT NOT NULL,
            steps INTEGER DEFAULT 0,
            lap_time_seconds INTEGER,
            mile_time_seconds INTEGER,
            
            UNIQUE(user_id, entry_date),
            FOREIGN KEY(user_id) REFERENCES Users(user_id)
            );
            
            CREATE TABLE IF NOT EXISTS Nutrition(
                nutrition_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                entry_date TEXT NOT NULL,
                calories REAL DEFAULT 0,
                protein REAL DEFAULT 0,
                carbs REAL DEFAULT 0,
                fat REAL DEFAULT 0,
                
                UNIQUE(user_id, entry_date),
                FOREIGN KEY(user_id) REFERENCES Users(user_id)
                );

            CREATE TABLE IF NOT EXISTS Sleep(
                sleep_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                entry_date TEXT NOT NULL,
                hours_slept REAL DEFAULT 0,
                
                UNIQUE(user_id, entry_date),
                FOREIGN KEY(user_id) REFERENCES Users(user_id)
                );

                CREATE TABLE IF NOT EXISTS Goals(
                goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                goal_text TEXT NOT NULL,
                
                FOREIGN KEY(user_id) REFERENCES Users(user_id)
                );
            )";
    //checking for errors in execution
    int result = sqlite3_exec(db, sql, nullptr, nullptr, &error_message);
    //checking to see if tables were created succesfully
    if(result != SQLITE_OK){
        cerr << "Table creation failed: "
             << error_message << std::endl;
                
        sqlite3_free(error_message);
        return false;
       }

    //Enabling foreign keys
    initialized = true;
    return true;
}

//saves steps fucntion that lets the user add steps to their existing daily count
bool Storage::save_steps(int user_id, const string& entry_date, int steps) {
    if(!initialized)
        return false;
    //sql code for the db to store steps
    const char* sql = R"(
        INSERT INTO Fitness(user_id, entry_date, steps)
        values (?,?,?) 
        
        ON CONFLICT(user_id, entry_date)
        DO UPDATE SET steps = COALESCE(Fitness.steps, 0) + excluded.steps;
        )";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2( db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    
    //Inserting the values into the sql statement
    sqlite3_bind_text( statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);

    sqlite3_bind_int(statement, 3, steps);
    //executes the statement
    result = sqlite3_step(statement);
    //cleans up/finalizes the statement
    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}

//Getter for steps 
FitnessData Storage::get_fitness(int user_id, const string& entry_date){
    //checking to make sure the database was initialized
    if(!initialized)
        return {0,0,0};
    //sql code for retreiving steps from the database
    const char* sql = R"(
    SELECT steps, lap_time_seconds, mile_time_seconds FROM Fitness WHERE user_id = ? AND entry_date = ?
    )";

    sqlite3_stmt* statement = nullptr;
    //prepares the sql statement
    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return {0,0,0};

    //inserting values for sql
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);

    //executes the sql statement
    result = sqlite3_steps(statement);
    //creating fitness data structure
    FitnessData fitness = {0,0,0};

    //check if a matching record was found and retrieve it
    if(result == SQLITE_ROW){
        fitness.steps = sqlite3_column_int(statement, 0);
        fitness.lap_time_seconds = sqlite3_column_int(statement, 1);
        fitness.mile_time_seconds = sqlite3_column_int(statement, 2);
    }

    //cleaning up the statement
    sqlite3_finalize(statement);

    return ;
}
