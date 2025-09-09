export const isHostname = (hostname: string): boolean => {
  const regex = new RegExp(
    /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/
  );
  return hostname === "localhost" || regex.test(hostname);
};

export const isValidUserName = (username: string): boolean => {
  const regex = new RegExp(/^[a-z]([-a-z0-9]{0,30})$/);
  return regex.test(username);
};
