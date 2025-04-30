import { Fragment } from 'react';
import MealsSummary from "./MealsSummary";
import AvailableMeals from "./AvailableMeals";

const Meals = props => {
    return <Fragment>
        <MealsSummary />
        <AvailableMeals />
    </Fragment>
}

export default Meals;