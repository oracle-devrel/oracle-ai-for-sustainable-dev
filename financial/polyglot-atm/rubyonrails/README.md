

sudo gem install rails
rails new oracle_rails_app --database=oracle
cd oracle_rails_app

gem 'activerecord-oracle_enhanced-adapter', '~> 6.1'

bundle install

