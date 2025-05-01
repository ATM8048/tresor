import '../../App.css';
import React, { useEffect, useState } from 'react';
import { getSecretsforUser } from "../../comunication/FetchSecrets";

/**
 * Secrets
 * @author Peter Rutschmann
 */
const Secrets = ({ loginValues }) => {
    const [secrets, setSecrets] = useState([]);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        const fetchSecrets = async () => {
            setErrorMessage('');
            if (!loginValues.email) {
                console.error('Secrets: No valid email, please do login first:' + loginValues);
                setErrorMessage("No valid email, please do login first.");
            } else {
                try {
                    const data = await getSecretsforUser(loginValues);
                    console.log(data);
                    setSecrets(data);
                } catch (error) {
                    console.error('Failed to fetch to server:', error.message);
                    setErrorMessage(error.message);
                }
            }
        };
        fetchSecrets();
    }, [loginValues]);

    return (
        <>
            <h1>my secrets</h1>
            {errorMessage && <p style={{ color: 'red' }}>{errorMessage}</p>}
            <form>
                <h2>Secrets</h2>
                <table border="1">
                    <thead>
                        <tr>
                            <th>Secret ID</th>
                            <th>User ID</th>
                            <th>Content</th>
                        </tr>
                    </thead>
                    <tbody>
                        {secrets?.length > 0 ? (
                            secrets.map(secret => {
                                let parsedContent;
                                try {
                                    // Falls content ein JSON-String ist
                                    parsedContent =
                                        typeof secret.content === "string"
                                            ? JSON.parse(secret.content)
                                            : secret.content;
                                } catch (e) {
                                    parsedContent = { error: "Invalid JSON" };
                                }

                                return (
                                    <tr key={secret.id}>
                                        <td>{secret.id}</td>
                                        <td>{secret.userId}</td>
                                        <td>
                                            <table>
                                                <tbody>
                                                    {Object.entries(parsedContent).map(([key, value]) => (
                                                        <tr key={key}>
                                                            <td style={{ fontWeight: "bold", paddingRight: "10px" }}>{key}</td>
                                                            <td>
                                                                {String(value)}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </td>
                                    </tr>
                                );
                            })
                        ) : (
                            <tr>
                                <td colSpan="3">No secrets available</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </form>
        </>
    );
};

export default Secrets;