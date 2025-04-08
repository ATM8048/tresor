import { useNavigate } from 'react-router-dom';
import { postLogin } from '../../comunication/FetchUser';

/**
 * LoginUser
 * @author Peter Rutschmann
 */
function LoginUser({ loginValues, setLoginValues }) {
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault()
        try {
            console.log(loginValues + "test")
            await postLogin(loginValues);
            navigate('/');
        } catch (error) {
            setLoginValues({ email: '', password: '' });
            console.error('Failed to fetch to server:', error.message);
        }
    };

    return (
        <div>
            <h2>Login user</h2>
            <form onSubmit={handleSubmit}>
                <section>
                    <aside>
                        <div>
                            <label>Email:</label>
                            <input
                                type="text"
                                value={loginValues.email}
                                onChange={(e) =>
                                    setLoginValues(prevValues => ({ ...prevValues, email: e.target.value }))}
                                required
                                placeholder="Please enter your email *"
                            />
                        </div>
                        <div>
                            <label>Password:</label>
                            <input
                                type="password"
                                value={loginValues.password}
                                onChange={(e) =>
                                    setLoginValues(prevValues => ({ ...prevValues, password: e.target.value }))}
                                required
                                placeholder="Please enter your password *"
                            />
                        </div>
                    </aside>
                </section>
                <button type="submit">Login</button>
            </form>
        </div>
    );
}

export default LoginUser;