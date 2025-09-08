```mermaid
graph TD
    subgraph "Data Layer"
        UserPreferencesRepository["UserPreferencesRepository"]
        CloudFirestore["Cloud Firestore"]
        FirebaseAuth["Firebase Authentication"]
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

    %% MainActivity instantiates UserPreferencesRepository
    UserPreferencesRepository -- "Instantiated in / Provided by" --> MainActivity

    %% ViewModel to Data/Auth Dependencies
    UserPreferencesRepository -- "Used by" --> AuthViewModel
    FirebaseAuth --> AuthViewModel
    CloudFirestore -- "Manages UserProfile" --> AuthViewModel
    UserProfileModel --> AuthViewModel

    FirebaseAuth --> FoodLogViewModel
    CloudFirestore -- "Manages Meal" --> FoodLogViewModel
    MealModel --> FoodLogViewModel
    MealSectionModel --> FoodLogViewModel

    FirebaseAuth --> QuestionsViewModel
    CloudFirestore -- "Manages User Answers (internal class)" --> QuestionsViewModel

    %% MainActivity and Navigation
    Screens -- "Receives ViewModels from" --> MainActivity
    NavRoutes -- "Is Used by" --> MainActivity
    AuthViewModel -- "Used by" --> Screens
    FoodLogViewModel -- "Used by" --> Screens
    QuestionsViewModel -- "Used by" --> Screens

    

    %% Class Definitions
    classDef data fill:#E6F2FA,stroke:#2E86C1,color:#000000,stroke-width:2px
    classDef model fill:#E0F8F1,stroke:#1ABC9C,color:#000000,stroke-width:2px
    classDef viewmodel fill:#FEF9E7,stroke:#F1C40F,color:#000000,stroke-width:2px
    classDef ui fill:#FDECEA,stroke:#E74C3C,color:#000000,stroke-width:2px
    classDef firebase fill:#FEF5E7,stroke:#F39C12,color:#000000,stroke-width:2px

    class UserPreferencesRepository,CloudFirestore,FirebaseAuth data
    class MealModel,MealSectionModel,UserProfileModel model
    class AuthViewModel,FoodLogViewModel,QuestionsViewModel viewmodel
    class MainActivity,Screens,NavRoutes ui
    class CloudFirestore,FirebaseAuth firebase

