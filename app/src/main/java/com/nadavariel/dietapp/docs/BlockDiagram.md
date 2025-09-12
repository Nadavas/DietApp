```mermaid
graph TD
    subgraph "Data Layer"
        UserPreferencesRepository["UserPreferencesRepository"]
        Firebase["Firebase"]
        ExternalAPI["External API (e.g., FatSecret)"]
    end

    subgraph "Model Layer"
        MealModel["Meal"]
        MealSectionModel["MealSection"]
        UserProfileModel["UserProfile"]
    end

    subgraph "ViewModel Layer"
        direction LR
        AuthViewModel["AuthViewModel"]
        FoodLogViewModel["FoodLogViewModel"]
        QuestionsViewModel["QuestionsViewModel"]
    end

    subgraph "UI Layer"
        direction TB
        MainActivity["MainActivity"]
        Screens["Screens"]
        NavRoutes["NavRoutes"]
    end

    UserPreferencesRepository --> MainActivity
    UserPreferencesRepository --> AuthViewModel
    Firebase --> AuthViewModel
    UserProfileModel --> AuthViewModel
    Firebase <--> FoodLogViewModel
    MealModel --> FoodLogViewModel
    MealSectionModel --> FoodLogViewModel
    Firebase --> QuestionsViewModel
    Firebase <--> ExternalAPI
    MainActivity --> Screens
    NavRoutes --> MainActivity
    AuthViewModel --> Screens
    FoodLogViewModel --> Screens
    QuestionsViewModel --> Screens

    classDef data fill:#E6F2FA,stroke:#2E86C1,color:#000000,stroke-width:2px
    classDef model fill:#E0F8F1,stroke:#1ABC9C,color:#000000,stroke-width:2px
    classDef viewmodel fill:#FEF9E7,stroke:#F1C40F,color:#000000,stroke-width:2px
    classDef ui fill:#FDECEA,stroke:#E74C3C,color:#000000,stroke-width:2px
    classDef firebase fill:#FEF5E7,stroke:#F39C12,color:#000000,stroke-width:2px
    classDef api fill:#FFD6A5,stroke:#FFA500,color:#000000,stroke-width:2px

    class UserPreferencesRepository,Firebase data
    class ExternalAPI api
    class MealModel,MealSectionModel,UserProfileModel model
    class AuthViewModel,FoodLogViewModel,QuestionsViewModel viewmodel
    class MainActivity,Screens,NavRoutes ui
    class Firebase firebase
