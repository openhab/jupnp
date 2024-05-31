import React from 'react';

function Home() {
  React.useEffect(() => {
    window.location.href = './docs/introduction';
  }, []);

  return null
}

export default Home;
