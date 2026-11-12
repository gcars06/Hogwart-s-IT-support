//This file is the header file for the c++ to SQLite class and it's functions
//that will allow us to store data and retrieve data from storage on a local 
//db file

#ifndef STORAGE_H
#define STORAGE_H

#include <sqlite3.h>
#include <vector>
#include <string>

//structure for our nutrition that holds multiple variables
struct NutritionData {
    double calories;
    double protein;
    double carbs;
    double fat;
};
//structure for Fitness to hold multiple variables
struct FitnessData{
    int steps;
    int lap_time_seconds;
    int mile_time_seconds;
}

class Storage { 
    public:
        //constructor and Destructor
        Storage();
        ~Storage();

        //preps the database
        bool initialize();

        // ----------UserID----------
        bool save_user_id(int user_id);
        bool check_user_id(int user_id);

        // ----------FITNESS----------
        //saving the user steps
        bool save_steps(int user_id, const std::string& entry_date, int steps);
        //Saving user lap times in seconds
        bool save_lap_time(int user_id, const std::string& entry_date, int seconds);
        //Saving user mile time in seconds
        bool save_mile_time(int user_id, const std::string& entry_date, in seconds);
        //retrieves ALL user fitness data
        FitnessData get_fitness(int user_id, const std::string& entry_date);
        // ----------NUTRITION----------
        //saves nutrition facts
        bool save_nutrition( int user_id, const std::string& entry_date, double calories, double protein, double carbs, double fat);
        //retreives ALL user nutrition facts 
        NutritionData get_nutrition(int user_id, const std::string& entry_date);
        // ----------SLEEP----------
        //saving hours slept
        bool save_hours_slept(int user_id, const std::string& entry_date, double hours_slept);
        //retrieves hours slept
        double get_hours_slept( int user_id, const std::string& entry_date);
        // ----------GOALS----------
        //saving goals 
        bool save_goal(int user_id, const std::string& goal_text);
        //retrieve goals
        std::vector<std::string> get_goals(int user_id);


        private:
            sqlite3* db;
            bool initialized;
};

#endif
