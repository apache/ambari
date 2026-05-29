import { faUser } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useEffect, useState } from "react";
import {
    Container,
    Navbar,
    Nav,
    Dropdown,
    DropdownDivider,
} from "react-bootstrap";
import {
    decryptData,
    getFromLocalStorage,
    parseJSONData
} from "./api/Utility.ts";
import { get } from "lodash";
import AmbariAboutModal from "./AmbariAboutModal";

type NavBarProps = {
    subPath: string;
    clusterName: string;
};

export default function NavBar({ subPath, clusterName }: NavBarProps) {
    const [showAmbariAboutModal, setShowAmbariAboutModal] = useState(false);
    const [loginUserName, setLoginUserName] = useState("");
    const [ambariLsVal, setAmbariLsVal] = useState(null);

    useEffect(() => {
        let ambariKey = getFromLocalStorage('ambari');
        if (ambariKey) {
            setAmbariLsVal(parseJSONData(decryptData(ambariKey)));
        }
    }, []);

    useEffect(() => {
        if (ambariLsVal) {
            const loginName = get(ambariLsVal, 'app.loginName');
            if (loginName) {
                setLoginUserName(loginName);
            }
        }
    }, [ambariLsVal]);

    return (
        <div>
            {showAmbariAboutModal ? (
                <AmbariAboutModal
                    isOpen={showAmbariAboutModal}
                    onClose={() => setShowAmbariAboutModal(false)}
                />
            ) : null}
            <Navbar collapseOnSelect expand="lg" className="bg-white">
                <Container className="d-flex justify-content-between">
                    <Navbar.Brand
                        className="text-black m-0 breadcrumb d-flex align-items-center"
                        style={{ fontSize: 24 }}
                    >
                        {" "}
                        Admin /
                        <div className="navbar-text ms-1" style={{ fontSize: 24 }}>
                            {subPath}
                        </div>
                    </Navbar.Brand>
                    <div className="d-flex align-items-center ">
                        <Nav.Link className="navbar-text navbar-size">
                            {clusterName}
                        </Nav.Link>
                        <Dropdown>
                            <Dropdown.Toggle
                                variant="transparent"
                                className="d-flex align-items-center border-0 ms-2"
                            >
                                <FontAwesomeIcon
                                    icon={faUser}
                                    className="me-1 navbar-text navbar-size"
                                />
                                <div className="navbar-text navbar-size">{loginUserName}</div>
                            </Dropdown.Toggle>

                            <Dropdown.Menu className="rounded-0">
                                <Dropdown.Item
                                    onClick={() => {
                                        setShowAmbariAboutModal(true);
                                    }}
                                >
                                    About
                                </Dropdown.Item>
                                <DropdownDivider />
                                <Dropdown.Item>Signout</Dropdown.Item>
                            </Dropdown.Menu>
                        </Dropdown>
                    </div>
                </Container>
            </Navbar>
        </div>
    );
}