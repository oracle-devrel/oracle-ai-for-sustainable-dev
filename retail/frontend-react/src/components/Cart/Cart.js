import { useContext } from 'react';
import CartContext from '../../store/cart-context';
import CartItem from './CartItem';

import Modal from '../UI/Modal';
import classes from './Cart.module.css';

const Cart = props => {
    const cartCtx = useContext(CartContext);

    const totalAmount = `$${cartCtx.totalAmount.toFixed(2)}`;
    const hasItems = cartCtx.items.length > 0;

    const cartItemRemoveHandler = id => {
        cartCtx.removeItem(id)
    };

    const cartItemAddHandler = item => {
        cartCtx.addItem({ ...item, amount: 1 });
    };

    const cartItems = <ul className={classes['cart-items']}>
        {cartCtx.items.map(item =>
            <CartItem
                key={item.id}
                name={item.name}
                amount={item.amount}
                price={item.price}
                onRemove={cartItemRemoveHandler.bind(null, item.id)}
                onAdd={cartItemAddHandler.bind(null, item)}
            />
        )}
    </ul>;

    const handleOrderButton = () => {
        const now = new Date()
        const orderId = now.getFullYear() + now.getMonth() + now.getDay() + now.getHours() + now.getMinutes() + now.getSeconds() + now.getMilliseconds()
        const request = {
            command: "order",
            serviceName: "order",
            orderId: orderId,
            orderItem: 'sushi',
            deliverTo: 'test address'

        }
        console.log(request);
    }

    return <Modal onClose={props.onClose}>
        {cartItems}
        <div className={classes.total}>
            <span>Total Amount</span>
            <span>{totalAmount}</span>
        </div>
        <div className={classes.actions}>
            <button className={classes['button--alt']} onClick={props.onClose}>Close</button>
            {hasItems && <button className={classes.button} onClick={handleOrderButton}>Order</button>}
        </div>
    </Modal>
}

export default Cart;