CREATE TABLE users (
                       username VARCHAR(50) PRIMARY KEY,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100),
                       role VARCHAR(20) DEFAULT 'BIDDER'
);

CREATE TABLE items (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255),
                       type VARCHAR(50),
                       start_price DOUBLE,
                       extra_info VARCHAR(255)
);

CREATE TABLE auctions (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          item_id INT,
                          start_time DATETIME,
                          end_time DATETIME,
                          current_price DOUBLE,
                          status VARCHAR(20),
                          FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE bid_transactions (
                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                  auction_id INT,
                                  username VARCHAR(50),
                                  amount DOUBLE,
                                  bid_time DATETIME,
                                  FOREIGN KEY (auction_id) REFERENCES auctions(id),
                                  FOREIGN KEY (username) REFERENCES users(username)
);