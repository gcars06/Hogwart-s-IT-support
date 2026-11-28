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
bool Storage::initialize(){
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
            user_id INTEGER PRIMARY KEY 
        );
            
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
    result = sqlite3_exec(db, sql, nullptr, nullptr, &error_message);
    //checking to see if tables were created succesfully
    if(result != SQLITE_OK){
        cerr << "Table creation failed: "
             << error_message << std::endl;
                
        sqlite3_free(error_message);
        return false;
       }

    initialized = true;
    return true;
}

bool Storage::save_user_id(int user_id){
    if(!initialized)
        return false;
    //sql for saving userID
    const char* sql = R"(
        INSERT OR IGNORE INTO Users(user_id)
        VALUES (?))";
    
    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2( db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    
    //Inserting the values into the sql statement
    sqlite3_bind_int(statement, 1, user_id);

    //executes the statement
    result = sqlite3_step(statement);
    //cleans up/finalizes the statement
    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}
//Check for users function
bool Storage::check_user_id(int user_id){
    if(!initialized)
        return false;
    //sql for checking userID
    const char* sql = R"(
        SELECT user_id FROM Users WHERE user_id = ?)";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2( db, sql, -1, &statement, nullptr);

    sqlite3_bind_int(statement, 1, user_id);

    result = sqlite3_step(statement);
    sqlite3_finalize(statement);

    return result == SQLITE_ROW;
}

//saves steps fucntion that replaces the attributes for our stepcount, the addition is handled by processing and not here
bool Storage::save_steps(int user_id, const string& entry_date, int steps) {
    if(!initialized)
        return false;
    //sql code for the db to store steps
    const char* sql = R"(
        INSERT INTO Fitness(user_id, entry_date, steps)
        values (?,?,?) 
        
        ON CONFLICT(user_id, entry_date)
        DO UPDATE SET steps = excluded.steps;
        )";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2( db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    
    //Inserting the values into the sql statement
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text( statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(statement, 3, steps);

    //executes the statement
    result = sqlite3_step(statement);
    //cleans up/finalizes the statement
    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}

bool Storage::save_lap_time(int user_id, const string& entry_date, int seconds){
    if(!initialized)
        return false;
const char* sql = R"(
    INSERT INTO Fitness(user_id, entry_date, lap_time_seconds)
    VALUES (?,?,?)

    ON CONFLICT(user_id, entry_date)
    DO UPDATE SET lap_time_seconds = excluded.lap_time_seconds;
    )";

sqlite3_stmt* statement = nullptr;

int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

if(result != SQLITE_OK)
    return false;

//Inserting the values into the sql statement
sqlite3_bind_int(statement, 1, user_id);
sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);
sqlite3_bind_int(statement, 3, seconds);

result = sqlite3_step(statement);

sqlite3_finalize(statement);
return result == SQLITE_DONE;
}

//setter for mile time
bool Storage::save_mile_time(int user_id, const string& entry_date, int seconds){
    if(!initialized)
        return false;
    const char* sql =R"(
    INSERT INTO Fitness(user_id, entry_date, mile_time_seconds)
    VALUES (?,?,?)
    
    ON CONFLICT(user_id,entry_date)
    DO UPDATE SET mile_time_seconds = excluded.mile_time_seconds;)";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    //insert values to sql statement
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(statement, 3, seconds);

    result = sqlite3_step(statement);

    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}

//Getter for steps 
FitnessData Storage::get_fitness(int user_id, const string& entry_date){
    //checking to make sure the database was initialized
    if(!initialized)
        return {-1,-1,-1};
    //sql code for retreiving steps from the database
    const char* sql = R"(
    SELECT steps, lap_time_seconds, mile_time_seconds FROM Fitness WHERE user_id = ? AND entry_date = ?
    )";

    sqlite3_stmt* statement = nullptr;
    //prepares the sql statement
    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return {-2,-2,-2};

    //inserting values for sql
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);

    //executes the sql statement
    result = sqlite3_step(statement);
    //creating fitness data structure
    FitnessData fitness = {-3,-3,-3};

    //check if a matching record was found and retrieve it
    if(result == SQLITE_ROW){
        fitness.steps = sqlite3_column_int(statement, 0);
        fitness.lap_time_seconds = sqlite3_column_int(statement, 1);
        fitness.mile_time_seconds = sqlite3_column_int(statement, 2);
    }

    //cleaning up the statement
    sqlite3_finalize(statement);

    return fitness;
}

//save Nutrition function
bool Storage::save_nutrition(int user_id, const std::string& entry_date, double calories, double protein, double carbs, double fat){
    if(!initialized)
        return false;
    const char* sql = R"(
    INSERT INTO Nutrition(user_id, entry_date, calories, protein, carbs, fat)
    VALUES(?1, ?2, max(?3, 0), max(?4, 0), max(?5, 0), max(?6, 0))

    ON CONFLICT(user_id,entry_date)
    DO UPDATE SET calories = CASE WHEN (?3 = -1) THEN calories ELSE ?3 END,
        protein = CASE WHEN (?4 = -1) THEN protein ELSE ?4 END,
        carbs = CASE WHEN (?5 = -1) THEN carbs ELSE ?5 END,
        fat = CASE WHEN (?6 = -1) THEN fat ELSE ?6 END;)";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    //insert values to sql statement
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_double(statement, 3, calories);
    sqlite3_bind_double(statement, 4, protein);
    sqlite3_bind_double(statement, 5, carbs);
    sqlite3_bind_double(statement, 6, fat);

    result = sqlite3_step(statement);

    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}

//getter for Nutrition data
NutritionData Storage::get_nutrition(int user_id, const string& entry_date){
    if(!initialized)
        return {-1,-1,-1,-1};
    //sql code for retreiving nutrition from the database
    const char* sql = R"(
    SELECT calories, protein, carbs, fat FROM Nutrition WHERE user_id = ? AND entry_date = ?
    )";

    sqlite3_stmt* statement = nullptr;
    //prepares the sql statement
    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return {-2,-2,-2,-2};

    //inserting values for sql
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);

    //executes the sql statement
    result = sqlite3_step(statement);
    //creating nutrition data structure
    NutritionData nutrition = {-3,-3,-3,-3};

    //check if a matching record was found and retrieve it
    if(result == SQLITE_ROW){
        nutrition.calories = sqlite3_column_double(statement, 0);
        nutrition.protein = sqlite3_column_double(statement, 1);
        nutrition.carbs = sqlite3_column_double(statement, 2);
        nutrition.fat = sqlite3_column_double(statement, 3);
    }

    //cleaning up the statement
    sqlite3_finalize(statement);

    return nutrition;
}
//saving hours slept 
bool Storage::save_hours_slept(int user_id, const std::string& entry_date, double hours_slept){
    if(!initialized)
        return false;
    const char* sql = R"(
        INSERT INTO Sleep(user_id, entry_date, hours_slept)
        VALUES(?,?,?)
        
        ON CONFLICT(user_id, entry_date)
        DO UPDATE SET hours_slept = excluded.hours_slept)";

    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return false;
    //insert values to sql statement
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_double(statement, 3, hours_slept);

    result = sqlite3_step(statement);

    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}
//getting hours slept from the data base
double Storage::get_hours_slept(int user_id, const std::string& entry_date){
    if(!initialized)
        return {-1};
    //sql code for retreiving nutrition from the database
    const char* sql = R"(
    SELECT hours_slept FROM Sleep WHERE user_id = ? AND entry_date = ?
    )";

    sqlite3_stmt* statement = nullptr;
    //prepares the sql statement
    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);

    if(result != SQLITE_OK)
        return {-2};

    //inserting values for sql
    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, entry_date.c_str(), -1, SQLITE_TRANSIENT);

    //executes the sql statement
    result = sqlite3_step(statement);

    double hours = -3;

    //checking to see if a row was found
    if(result == SQLITE_ROW){
        hours = sqlite3_column_double(statement, 0);
    }

    sqlite3_finalize(statement);

    return hours;
}

//modifed by Gabriel to resolve building errors I was getting. Double check that this is correct and works as intended. (was ai generated as I was getting irritated)


// Adds one goal to the authenticated user's goal list.
bool Storage::save_goal(int user_id, const std::string& goal_text){
    if(!initialized)
        return false;

    // Bind both values so goal text is stored as data, not interpreted as SQL.
    const char* sql = R"(
        INSERT INTO Goals(user_id, goal_text)
        VALUES (?, ?))";
    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);
    if(result != SQLITE_OK)
        return false;

    sqlite3_bind_int(statement, 1, user_id);
    sqlite3_bind_text(statement, 2, goal_text.c_str(), -1, SQLITE_TRANSIENT);
    result = sqlite3_step(statement);
    sqlite3_finalize(statement);
    return result == SQLITE_DONE;
}

// Returns the user's goals in the order they were saved.
vector<string> Storage::get_goals(int user_id){
    vector<string> goals;
    if(!initialized)
        return goals;

    // Preserve insertion order so callers receive goals consistently.
    const char* sql = R"(
        SELECT goal_text FROM Goals
        WHERE user_id = ?
        ORDER BY goal_id)";
    sqlite3_stmt* statement = nullptr;

    int result = sqlite3_prepare_v2(db, sql, -1, &statement, nullptr);
    if(result != SQLITE_OK){
        cerr << "Database error retrieving goals: " << sqlite3_errmsg(db) << endl;
        return goals;
    }

    sqlite3_bind_int(statement, 1, user_id);
    while((result = sqlite3_step(statement)) == SQLITE_ROW){
        const unsigned char* text = sqlite3_column_text(statement, 0);
        goals.emplace_back(text ? reinterpret_cast<const char*>(text) : "");
    }

    sqlite3_finalize(statement);
    if(result != SQLITE_DONE){
        cerr << "Database error retrieving goals: " << sqlite3_errmsg(db) << endl;
        goals.clear();
    }

    return goals;
}
