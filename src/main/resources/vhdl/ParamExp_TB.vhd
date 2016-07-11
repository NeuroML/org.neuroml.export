library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use std.textio.all;
use ieee.std_logic_textio.all; -- if you're saving this type of signal
entity paramexp_tb is
end paramexp_tb;

architecture tb of paramexp_tb is

component ParamExp is
	generic( 
		BIT_TOP 	: integer := 20;	
		BIT_BOTTOM	: integer := -20);	
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		X		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		Output	: Out sfixed(BIT_TOP downto BIT_BOTTOM)
		);
end component;


signal clk 			: std_logic := '0';
signal rst 			: std_logic := '0';
signal Start 			: std_logic := '0';
signal Done 			: std_logic := '0';
signal X 			: sfixed(20 downto -20);
signal Output 			: sfixed(20 downto -20);
begin
	
	ParamExp_uut : ParamExp 
	generic map( 
		BIT_TOP 	=> 20,
		BIT_BOTTOM	=> -20)
    port map (	clk => clk,
				rst => rst,
				Start => Start,
				Done => Done,
				X => X,
				Output => Output
				);
		

	process
	begin
	wait for 10ns;
	clk <= not(clk);
	wait for 10ns;
	clk <= not(clk);
	end process;
	
	process (Done) 
	begin
		if Done'event and Done = '1' then
			report "The value of exp( " & real'image(to_real(X)) & " ) = " & real'image(to_real(Output));
		end if;
	end process;
		
	process 
	begin            
	   -- wait for Reset to complete
	   -- wait until rst='1';
	   rst<='1';
	   wait for 40 ns;
	   rst<='0';
	   wait for 40 ns;
		X <= to_sfixed(0.01,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(0.1,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(1,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(6.4,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(12.9,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(4.6,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(4.5,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		X <= to_sfixed(4.49,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		
		X <= to_sfixed(-0.001,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(-0.01,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(-1.567,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(-6,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(-10.4,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
		
		X <= to_sfixed(-14,20,-20);
	   wait for 20 ns;
		Start <= '1';
	   wait for 20 ns;
		Start <= '0'; 
	   wait for 1000 ns;
    end process;
	
end tb;
		


